package com.consultancy.platform.modules.consultants;

import com.consultancy.platform.common.config.DatabaseSupport;
import com.consultancy.platform.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.*;
import java.util.*;

@Service
public class ConsultantService {
    private final JdbcClient jdbc;
    private final DatabaseSupport db;

    public ConsultantService(JdbcClient jdbc, DatabaseSupport db) {
        this.jdbc = jdbc;
        this.db = db;
    }

    @Transactional
    public ConsultantDtos.ConsultantResponse upsertProfile(String userPublicId, ConsultantDtos.UpsertProfileRequest request) {
        long userId = db.userId(userPublicId);
        String publicId = jdbc.sql("SELECT public_id FROM consultant_profiles WHERE user_id = :userId AND deleted_at IS NULL")
                .param("userId", userId)
                .query(String.class)
                .optional()
                .orElseGet(db::uuid);
        jdbc.sql("""
                        INSERT INTO consultant_profiles(public_id, user_id, headline, bio, timezone, default_price_amount, currency, approval_status)
                        VALUES (:publicId, :userId, :headline, :bio, :timezone, :price, :currency, 'APPROVED')
                        ON DUPLICATE KEY UPDATE headline = VALUES(headline), bio = VALUES(bio), timezone = VALUES(timezone),
                          default_price_amount = VALUES(default_price_amount), currency = VALUES(currency), deleted_at = NULL
                        """)
                .param("publicId", publicId)
                .param("userId", userId)
                .param("headline", request.headline())
                .param("bio", request.bio())
                .param("timezone", request.timezone())
                .param("price", request.defaultPriceAmount())
                .param("currency", request.currency())
                .update();
        db.grantRole(userId, "CONSULTANT");
        return profile(publicId);
    }

    public List<ConsultantDtos.ConsultantResponse> listConsultants(int page, int size) {
        return jdbc.sql("""
                        SELECT cp.public_id, u.public_id user_public_id, u.display_name, cp.headline, cp.bio, cp.timezone,
                               cp.default_price_amount, cp.currency
                        FROM consultant_profiles cp JOIN users u ON u.id = cp.user_id
                        WHERE cp.deleted_at IS NULL AND cp.approval_status = 'APPROVED'
                        ORDER BY cp.created_at DESC LIMIT :limit OFFSET :offset
                        """)
                .param("limit", size)
                .param("offset", page * size)
                .query(this::profileRow)
                .list();
    }

    public ConsultantDtos.ConsultantResponse profile(String consultantPublicId) {
        return jdbc.sql("""
                        SELECT cp.public_id, u.public_id user_public_id, u.display_name, cp.headline, cp.bio, cp.timezone,
                               cp.default_price_amount, cp.currency
                        FROM consultant_profiles cp JOIN users u ON u.id = cp.user_id
                        WHERE cp.public_id = :publicId AND cp.deleted_at IS NULL
                        """)
                .param("publicId", consultantPublicId)
                .query(this::profileRow)
                .optional()
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Consultant not found"));
    }

    @Transactional
    public String addAvailability(String userPublicId, ConsultantDtos.AvailabilityRuleRequest request) {
        long consultantId = consultantIdForUser(userPublicId);
        String publicId = db.uuid();
        jdbc.sql("""
                        INSERT INTO availability_rules(public_id, consultant_id, timezone, start_date, end_date, start_time, end_time,
                          slot_duration_minutes, buffer_before_minutes, buffer_after_minutes, recurrence_frequency,
                          recurrence_interval, days_of_week)
                        VALUES (:publicId, :consultantId, :timezone, :startDate, :endDate, :startTime, :endTime,
                          :duration, :bufferBefore, :bufferAfter, :frequency, :interval, :days)
                        """)
                .param("publicId", publicId)
                .param("consultantId", consultantId)
                .param("timezone", request.timezone())
                .param("startDate", request.startDate())
                .param("endDate", request.endDate())
                .param("startTime", request.startTime())
                .param("endTime", request.endTime())
                .param("duration", request.slotDurationMinutes())
                .param("bufferBefore", request.bufferBeforeMinutes())
                .param("bufferAfter", request.bufferAfterMinutes())
                .param("frequency", request.recurrenceFrequency())
                .param("interval", request.recurrenceInterval())
                .param("days", request.daysOfWeek() == null ? "" : String.join(",", request.daysOfWeek()))
                .update();
        return publicId;
    }

    public List<ConsultantDtos.SlotResponse> availableSlots(String consultantPublicId, Instant from, Instant to) {
        long consultantId = db.consultantId(consultantPublicId);
        ConsultantDtos.ConsultantResponse profile = profile(consultantPublicId);
        List<Rule> rules = jdbc.sql("SELECT * FROM availability_rules WHERE consultant_id = :consultantId AND active = TRUE AND deleted_at IS NULL")
                .param("consultantId", consultantId)
                .query(this::ruleRow)
                .list();
        List<ConsultantDtos.SlotResponse> slots = new ArrayList<>();
        for (Rule rule : rules) {
            ZoneId zone = ZoneId.of(rule.timezone());
            LocalDate start = max(rule.startDate(), LocalDateTime.ofInstant(from, zone).toLocalDate());
            LocalDate end = min(rule.endDate() == null ? start.plusDays(90) : rule.endDate(), LocalDateTime.ofInstant(to, zone).toLocalDate());
            for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
                if (!rule.matches(day)) {
                    continue;
                }
                LocalDateTime cursor = LocalDateTime.of(day, rule.startTime());
                LocalDateTime dayEnd = LocalDateTime.of(day, rule.endTime());
                while (!cursor.plusMinutes(rule.slotDurationMinutes()).isAfter(dayEnd)) {
                    Instant startsAt = cursor.atZone(zone).toInstant();
                    Instant endsAt = cursor.plusMinutes(rule.slotDurationMinutes()).atZone(zone).toInstant();
                    if (!startsAt.isBefore(from) && endsAt.isBefore(to.plusMillis(1)) && !hasOverlap(consultantId, startsAt, endsAt)) {
                        slots.add(new ConsultantDtos.SlotResponse(db.uuid(), startsAt, endsAt, rule.timezone(), profile.defaultPriceAmount(), profile.currency()));
                    }
                    cursor = cursor.plusMinutes(rule.slotDurationMinutes() + rule.bufferAfterMinutes());
                }
            }
        }
        return slots.stream().sorted(Comparator.comparing(ConsultantDtos.SlotResponse::startsAt)).limit(200).toList();
    }

    @Transactional
    public ConsultantDtos.MeetingResponse createSeminar(String userPublicId, ConsultantDtos.SeminarRequest request) {
        long consultantId = consultantIdForUser(userPublicId);
        if (!request.startsAt().isBefore(request.endsAt())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "startsAt must be before endsAt");
        }
        if (hasOverlap(consultantId, request.startsAt(), request.endsAt())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Consultant already has a meeting in this time window");
        }
        String meetingPublicId = db.uuid();
        jdbc.sql("""
                        INSERT INTO meetings(public_id, consultant_id, title, description, meeting_type, status, timezone,
                          starts_at, ends_at, price_amount, currency)
                        VALUES (:publicId, :consultantId, :title, :description, 'SEMINAR', 'PUBLISHED', :timezone,
                          :startsAt, :endsAt, :price, :currency)
                        """)
                .param("publicId", meetingPublicId)
                .param("consultantId", consultantId)
                .param("title", request.title())
                .param("description", request.description())
                .param("timezone", request.timezone())
                .param("startsAt", request.startsAt())
                .param("endsAt", request.endsAt())
                .param("price", request.priceAmount())
                .param("currency", request.currency())
                .update();
        long meetingId = db.meetingId(meetingPublicId);
        jdbc.sql("INSERT INTO seminar_sessions(meeting_id, max_participants) VALUES (:meetingId, :max)")
                .param("meetingId", meetingId)
                .param("max", request.maxParticipants())
                .update();
        return meeting(meetingPublicId);
    }

    public List<ConsultantDtos.MeetingResponse> listSeminars(int page, int size) {
        return jdbc.sql("""
                        SELECT m.*, ss.max_participants, ss.confirmed_count
                        FROM meetings m JOIN seminar_sessions ss ON ss.meeting_id = m.id
                        WHERE m.meeting_type = 'SEMINAR' AND m.deleted_at IS NULL AND m.status IN ('PUBLISHED','CONFIRMED','LIVE')
                        ORDER BY m.starts_at ASC LIMIT :limit OFFSET :offset
                        """)
                .param("limit", size)
                .param("offset", page * size)
                .query(this::meetingRow)
                .list();
    }

    public ConsultantDtos.MeetingResponse meeting(String meetingPublicId) {
        return jdbc.sql("""
                        SELECT m.*, ss.max_participants, ss.confirmed_count
                        FROM meetings m LEFT JOIN seminar_sessions ss ON ss.meeting_id = m.id
                        WHERE m.public_id = :publicId AND m.deleted_at IS NULL
                        """)
                .param("publicId", meetingPublicId)
                .query(this::meetingRow)
                .single();
    }

    private long consultantIdForUser(String userPublicId) {
        long userId = db.userId(userPublicId);
        return jdbc.sql("SELECT id FROM consultant_profiles WHERE user_id = :userId AND deleted_at IS NULL")
                .param("userId", userId)
                .query(Long.class)
                .optional()
                .orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "Consultant profile is required"));
    }

    private boolean hasOverlap(long consultantId, Instant startsAt, Instant endsAt) {
        Long count = jdbc.sql("""
                        SELECT COUNT(*) FROM meetings
                        WHERE consultant_id = :consultantId AND deleted_at IS NULL
                          AND status IN ('PUBLISHED','PENDING_PAYMENT','CONFIRMED','LIVE')
                          AND starts_at < :endsAt AND ends_at > :startsAt
                        """)
                .param("consultantId", consultantId)
                .param("startsAt", startsAt)
                .param("endsAt", endsAt)
                .query(Long.class)
                .single();
        return count > 0;
    }

    private ConsultantDtos.ConsultantResponse profileRow(ResultSet rs, int rowNum) throws SQLException {
        return new ConsultantDtos.ConsultantResponse(
                rs.getString("public_id"),
                rs.getString("user_public_id"),
                rs.getString("display_name"),
                rs.getString("headline"),
                rs.getString("bio"),
                rs.getString("timezone"),
                rs.getLong("default_price_amount"),
                rs.getString("currency")
        );
    }

    private ConsultantDtos.MeetingResponse meetingRow(ResultSet rs, int rowNum) throws SQLException {
        Integer max = rs.getObject("max_participants") == null ? null : rs.getInt("max_participants");
        Integer confirmed = rs.getObject("confirmed_count") == null ? null : rs.getInt("confirmed_count");
        return new ConsultantDtos.MeetingResponse(
                rs.getString("public_id"),
                rs.getString("title"),
                rs.getString("meeting_type"),
                rs.getString("status"),
                rs.getTimestamp("starts_at").toInstant(),
                rs.getTimestamp("ends_at").toInstant(),
                rs.getLong("price_amount"),
                rs.getString("currency"),
                max,
                confirmed
        );
    }

    private Rule ruleRow(ResultSet rs, int rowNum) throws SQLException {
        Set<DayOfWeek> days = new HashSet<>();
        String rawDays = rs.getString("days_of_week");
        if (rawDays != null && !rawDays.isBlank()) {
            for (String day : rawDays.split(",")) {
                days.add(DayOfWeek.valueOf(day.trim().toUpperCase(Locale.ROOT)));
            }
        }
        return new Rule(
                rs.getString("timezone"),
                rs.getDate("start_date").toLocalDate(),
                rs.getDate("end_date") == null ? null : rs.getDate("end_date").toLocalDate(),
                rs.getTime("start_time").toLocalTime(),
                rs.getTime("end_time").toLocalTime(),
                rs.getInt("slot_duration_minutes"),
                rs.getInt("buffer_after_minutes"),
                rs.getString("recurrence_frequency"),
                days
        );
    }

    private LocalDate max(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private LocalDate min(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }

    private record Rule(String timezone, LocalDate startDate, LocalDate endDate, LocalTime startTime, LocalTime endTime,
                        int slotDurationMinutes, int bufferAfterMinutes, String frequency, Set<DayOfWeek> days) {
        boolean matches(LocalDate day) {
            if ("DAILY".equalsIgnoreCase(frequency)) {
                return true;
            }
            return days.isEmpty() || days.contains(day.getDayOfWeek());
        }
    }
}
