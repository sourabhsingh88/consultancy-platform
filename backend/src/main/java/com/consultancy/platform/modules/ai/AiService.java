package com.consultancy.platform.modules.ai;

import com.consultancy.platform.common.config.DatabaseSupport;
import com.consultancy.platform.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AiService {
    private final JdbcClient jdbc;
    private final DatabaseSupport db;
    private final AiProvider aiProvider;

    public AiService(JdbcClient jdbc, DatabaseSupport db, AiProvider aiProvider) {
        this.jdbc = jdbc;
        this.db = db;
        this.aiProvider = aiProvider;
    }

    @Transactional
    public AiDtos.SummaryResponse generateSummary(String userPublicId, String meetingPublicId) {
        long userId = db.userId(userPublicId);
        long meetingId = db.meetingId(meetingPublicId);
        if (!canAccess(userId, meetingId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Not allowed");
        }
        String title = jdbc.sql("SELECT title FROM meetings WHERE id = :meetingId").param("meetingId", meetingId).query(String.class).single();
        String context = context(meetingId);
        AiDtos.SummaryResponse generated = aiProvider.summarize(title, context);
        String publicId = db.uuid();
        jdbc.sql("""
                        INSERT INTO ai_summaries(public_id, meeting_id, provider, status, summary, key_points, action_items, follow_up_notes)
                        VALUES (:publicId, :meetingId, 'AI', 'COMPLETED', :summary, JSON_ARRAY(), JSON_ARRAY(), :followUp)
                        """)
                .param("publicId", publicId)
                .param("meetingId", meetingId)
                .param("summary", generated.summary())
                .param("followUp", generated.followUpNotes())
                .update();
        return new AiDtos.SummaryResponse(publicId, "COMPLETED", generated.summary(), generated.keyPoints(), generated.actionItems(), generated.followUpNotes());
    }

    public AiDtos.SummaryResponse latestSummary(String userPublicId, String meetingPublicId) {
        long userId = db.userId(userPublicId);
        long meetingId = db.meetingId(meetingPublicId);
        if (!canAccess(userId, meetingId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Not allowed");
        }
        return jdbc.sql("SELECT public_id, status, summary, follow_up_notes FROM ai_summaries WHERE meeting_id = :meetingId ORDER BY created_at DESC LIMIT 1")
                .param("meetingId", meetingId)
                .query((rs, row) -> new AiDtos.SummaryResponse(rs.getString("public_id"), rs.getString("status"), rs.getString("summary"), List.of(), List.of(), rs.getString("follow_up_notes")))
                .optional()
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Summary not generated yet"));
    }

    public AiDtos.SupportAnswerResponse answer(String userPublicId, String meetingPublicId, AiDtos.SupportQuestionRequest request) {
        long userId = db.userId(userPublicId);
        long meetingId = db.meetingId(meetingPublicId);
        if (!canAccess(userId, meetingId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Not allowed");
        }
        return aiProvider.answer(request.question(), context(meetingId));
    }

    private String context(long meetingId) {
        return String.join("\n", jdbc.sql("""
                        SELECT CONCAT(u.display_name, ': ', cm.body)
                        FROM chat_messages cm JOIN users u ON u.id = cm.sender_id
                        WHERE cm.meeting_id = :meetingId AND cm.deleted_at IS NULL
                        ORDER BY cm.id ASC LIMIT 500
                        """)
                .param("meetingId", meetingId)
                .query(String.class)
                .list());
    }

    private boolean canAccess(long userId, long meetingId) {
        return jdbc.sql("""
                        SELECT COUNT(*) FROM bookings WHERE user_id = :userId AND meeting_id = :meetingId
                        UNION ALL
                        SELECT COUNT(*) FROM meetings m JOIN consultant_profiles cp ON cp.id = m.consultant_id WHERE m.id = :meetingId AND cp.user_id = :userId
                        """)
                .param("userId", userId)
                .param("meetingId", meetingId)
                .query(Long.class)
                .list()
                .stream()
                .mapToLong(Long::longValue)
                .sum() > 0;
    }
}
