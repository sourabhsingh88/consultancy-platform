package com.consultancy.platform.modules.bookings;

import com.consultancy.platform.common.config.DatabaseSupport;
import com.consultancy.platform.common.exception.BusinessException;
import com.consultancy.platform.modules.payments.PaymentDtos;
import com.consultancy.platform.modules.payments.RazorpayClient;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class BookingService {
    private final JdbcClient jdbc;
    private final DatabaseSupport db;
    private final RazorpayClient razorpayClient;

    public BookingService(JdbcClient jdbc, DatabaseSupport db, RazorpayClient razorpayClient) {
        this.jdbc = jdbc;
        this.db = db;
        this.razorpayClient = razorpayClient;
    }

    @Transactional
    public BookingDtos.BookingResponse bookConsultation(String userPublicId, String idempotencyKey, BookingDtos.CreateConsultationRequest request) {
        long userId = db.userId(userPublicId);
        Long existing = existingBooking(userId, idempotencyKey);
        if (existing != null) {
            return response(existing);
        }
        long consultantId = db.consultantId(request.consultantPublicId());
        if (!request.startsAt().isBefore(request.endsAt())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "startsAt must be before endsAt");
        }
        lockOverlappingMeetings(consultantId, request.startsAt(), request.endsAt());
        if (hasOverlap(consultantId, request.startsAt(), request.endsAt())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Slot is no longer available");
        }
        var pricing = jdbc.sql("SELECT default_price_amount, currency, timezone FROM consultant_profiles WHERE id = :id")
                .param("id", consultantId)
                .query((rs, row) -> new Pricing(rs.getLong("default_price_amount"), rs.getString("currency"), rs.getString("timezone")))
                .single();
        String meetingPublicId = db.uuid();
        jdbc.sql("""
                        INSERT INTO meetings(public_id, consultant_id, title, meeting_type, status, timezone, starts_at, ends_at, price_amount, currency)
                        VALUES (:publicId, :consultantId, 'Private consultation', 'ONE_TO_ONE', 'PENDING_PAYMENT', :timezone, :startsAt, :endsAt, :price, :currency)
                        """)
                .param("publicId", meetingPublicId)
                .param("consultantId", consultantId)
                .param("timezone", pricing.timezone())
                .param("startsAt", request.startsAt())
                .param("endsAt", request.endsAt())
                .param("price", pricing.amount())
                .param("currency", pricing.currency())
                .update();
        long meetingId = db.meetingId(meetingPublicId);
        String bookingPublicId = db.uuid();
        jdbc.sql("""
                        INSERT INTO bookings(public_id, meeting_id, user_id, status, approval_status, idempotency_key, notes)
                        VALUES (:publicId, :meetingId, :userId, :status, 'PENDING', :key, :notes)
                        """)
                .param("publicId", bookingPublicId)
                .param("meetingId", meetingId)
                .param("userId", userId)
                .param("status", pricing.amount() == 0 ? "CONFIRMED" : "PAYMENT_PENDING")
                .param("key", idempotencyKey)
                .param("notes", request.notes())
                .update();
        if (pricing.amount() == 0) {
            jdbc.sql("UPDATE meetings SET status = 'CONFIRMED' WHERE id = :meetingId").param("meetingId", meetingId).update();
        }
        long bookingId = bookingId(bookingPublicId);
        PaymentDtos.PaymentOrderResponse order = createPaymentIfNeeded(bookingId, bookingPublicId, pricing.amount(), pricing.currency());
        return new BookingDtos.BookingResponse(bookingPublicId, meetingPublicId, pricing.amount() == 0 ? "CONFIRMED" : "PAYMENT_PENDING", order);
    }

    @Transactional
    public BookingDtos.BookingResponse registerSeminar(String userPublicId, String seminarPublicId, String idempotencyKey) {
        long userId = db.userId(userPublicId);
        Long existing = existingBooking(userId, idempotencyKey);
        if (existing != null) {
            return response(existing);
        }
        long meetingId = db.meetingId(seminarPublicId);
        jdbc.sql("SELECT id FROM seminar_sessions WHERE meeting_id = :meetingId FOR UPDATE").param("meetingId", meetingId).query(Long.class).single();
        var seminar = jdbc.sql("""
                        SELECT m.price_amount, m.currency, ss.confirmed_count, ss.max_participants
                        FROM meetings m JOIN seminar_sessions ss ON ss.meeting_id = m.id
                        WHERE m.id = :meetingId
                        """)
                .param("meetingId", meetingId)
                .query((rs, row) -> new SeminarPricing(rs.getLong("price_amount"), rs.getString("currency"), rs.getInt("confirmed_count"), rs.getInt("max_participants")))
                .single();
        if (seminar.confirmedCount() >= seminar.maxParticipants()) {
            throw new BusinessException(HttpStatus.CONFLICT, "Seminar is full");
        }
        String bookingPublicId = db.uuid();
        jdbc.sql("""
                        INSERT INTO bookings(public_id, meeting_id, user_id, status, approval_status, idempotency_key)
                        VALUES (:publicId, :meetingId, :userId, :status, 'APPROVED', :key)
                        """)
                .param("publicId", bookingPublicId)
                .param("meetingId", meetingId)
                .param("userId", userId)
                .param("status", seminar.amount() == 0 ? "CONFIRMED" : "PAYMENT_PENDING")
                .param("key", idempotencyKey)
                .update();
        if (seminar.amount() == 0) {
            jdbc.sql("UPDATE seminar_sessions SET confirmed_count = confirmed_count + 1 WHERE meeting_id = :meetingId").param("meetingId", meetingId).update();
        }
        long bookingId = bookingId(bookingPublicId);
        PaymentDtos.PaymentOrderResponse order = createPaymentIfNeeded(bookingId, bookingPublicId, seminar.amount(), seminar.currency());
        return new BookingDtos.BookingResponse(bookingPublicId, seminarPublicId, seminar.amount() == 0 ? "CONFIRMED" : "PAYMENT_PENDING", order);
    }

    @Transactional
    public void cancel(String userPublicId, String bookingPublicId, String reason) {
        long userId = db.userId(userPublicId);
        jdbc.sql("""
                        UPDATE bookings SET status = 'CANCELLED', cancelled_reason = :reason
                        WHERE public_id = :bookingPublicId AND user_id = :userId AND status IN ('PAYMENT_PENDING','CONFIRMED')
                        """)
                .param("reason", reason)
                .param("bookingPublicId", bookingPublicId)
                .param("userId", userId)
                .update();
    }

    private PaymentDtos.PaymentOrderResponse createPaymentIfNeeded(long bookingId, String bookingPublicId, long amount, String currency) {
        if (amount == 0) {
            return null;
        }
        String orderId = razorpayClient.createOrder(amount, currency, bookingPublicId);
        jdbc.sql("""
                        INSERT INTO payments(public_id, booking_id, provider, provider_order_id, amount, currency, status)
                        VALUES (:publicId, :bookingId, 'RAZORPAY', :orderId, :amount, :currency, 'CREATED')
                        """)
                .param("publicId", db.uuid())
                .param("bookingId", bookingId)
                .param("orderId", orderId)
                .param("amount", amount)
                .param("currency", currency)
                .update();
        return new PaymentDtos.PaymentOrderResponse("RAZORPAY", orderId, amount, currency, razorpayClient.keyId());
    }

    private BookingDtos.BookingResponse response(long bookingId) {
        return jdbc.sql("""
                        SELECT b.public_id booking_public_id, m.public_id meeting_public_id, b.status,
                          p.provider, p.provider_order_id, p.amount, p.currency
                        FROM bookings b JOIN meetings m ON m.id = b.meeting_id
                        LEFT JOIN payments p ON p.booking_id = b.id
                        WHERE b.id = :bookingId
                        """)
                .param("bookingId", bookingId)
                .query((rs, row) -> new BookingDtos.BookingResponse(
                        rs.getString("booking_public_id"),
                        rs.getString("meeting_public_id"),
                        rs.getString("status"),
                        rs.getString("provider_order_id") == null ? null : new PaymentDtos.PaymentOrderResponse(
                                rs.getString("provider"),
                                rs.getString("provider_order_id"),
                                rs.getLong("amount"),
                                rs.getString("currency"),
                                razorpayClient.keyId()
                        )
                ))
                .single();
    }

    private Long existingBooking(long userId, String idempotencyKey) {
        return jdbc.sql("SELECT id FROM bookings WHERE user_id = :userId AND idempotency_key = :key")
                .param("userId", userId)
                .param("key", idempotencyKey)
                .query(Long.class)
                .optional()
                .orElse(null);
    }

    private long bookingId(String publicId) {
        return jdbc.sql("SELECT id FROM bookings WHERE public_id = :publicId").param("publicId", publicId).query(Long.class).single();
    }

    private void lockOverlappingMeetings(long consultantId, Instant startsAt, Instant endsAt) {
        jdbc.sql("SELECT id FROM meetings WHERE consultant_id = :consultantId AND starts_at < :endsAt AND ends_at > :startsAt FOR UPDATE")
                .param("consultantId", consultantId).param("startsAt", startsAt).param("endsAt", endsAt).query(Long.class).list();
    }

    private boolean hasOverlap(long consultantId, Instant startsAt, Instant endsAt) {
        return jdbc.sql("""
                        SELECT COUNT(*) FROM meetings
                        WHERE consultant_id = :consultantId AND deleted_at IS NULL
                          AND status IN ('PUBLISHED','PENDING_PAYMENT','CONFIRMED','LIVE')
                          AND starts_at < :endsAt AND ends_at > :startsAt
                        """)
                .param("consultantId", consultantId).param("startsAt", startsAt).param("endsAt", endsAt)
                .query(Long.class).single() > 0;
    }

    private record Pricing(long amount, String currency, String timezone) {
    }

    private record SeminarPricing(long amount, String currency, int confirmedCount, int maxParticipants) {
    }
}
