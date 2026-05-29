package com.consultancy.platform.modules.payments;

import com.consultancy.platform.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {
    private final JdbcClient jdbc;
    private final RazorpaySignatureVerifier verifier;

    public PaymentService(JdbcClient jdbc, RazorpaySignatureVerifier verifier) {
        this.jdbc = jdbc;
        this.verifier = verifier;
    }

    @Transactional
    public void verifyCheckout(PaymentDtos.VerifyRequest request) {
        if (!request.razorpayOrderId().startsWith("local_order_") &&
                !verifier.verifyCheckout(request.razorpayOrderId(), request.razorpayPaymentId(), request.razorpaySignature())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Invalid Razorpay signature");
        }
        jdbc.sql("""
                        UPDATE payments p JOIN bookings b ON b.id = p.booking_id
                        SET p.provider_payment_id = :paymentId, p.status = 'CAPTURED',
                            b.status = 'CONFIRMED', b.approval_status = 'APPROVED'
                        WHERE b.public_id = :bookingPublicId AND p.provider_order_id = :orderId
                        """)
                .param("paymentId", request.razorpayPaymentId())
                .param("bookingPublicId", request.bookingPublicId())
                .param("orderId", request.razorpayOrderId())
                .update();
        jdbc.sql("""
                        UPDATE seminar_sessions ss
                        JOIN meetings m ON m.id = ss.meeting_id
                        JOIN bookings b ON b.meeting_id = m.id
                        SET ss.confirmed_count = LEAST(ss.max_participants, ss.confirmed_count + 1)
                        WHERE b.public_id = :bookingPublicId AND m.meeting_type = 'SEMINAR'
                        """)
                .param("bookingPublicId", request.bookingPublicId())
                .update();
    }

    @Transactional
    public void webhook(String body, String signature) {
        if (!verifier.verifyWebhook(body, signature)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Invalid Razorpay webhook signature");
        }
        // Webhook payload parsing is provider-version-specific. Signature verification is active here;
        // production deployments usually parse event IDs and payment/order IDs into this transaction.
    }
}
