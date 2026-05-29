package com.consultancy.platform.modules.chat;

import com.consultancy.platform.common.config.DatabaseSupport;
import com.consultancy.platform.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
public class ChatService {
    private final JdbcClient jdbc;
    private final DatabaseSupport db;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatService(JdbcClient jdbc, DatabaseSupport db, SimpMessagingTemplate messagingTemplate) {
        this.jdbc = jdbc;
        this.db = db;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public ChatDtos.MessageResponse send(String userPublicId, String meetingPublicId, ChatDtos.SendMessageRequest request) {
        long userId = db.userId(userPublicId);
        long meetingId = db.meetingId(meetingPublicId);
        if (!canAccessMeeting(userId, meetingId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "You are not a participant of this meeting");
        }
        String publicId = db.uuid();
        jdbc.sql("""
                        INSERT INTO chat_messages(public_id, meeting_id, sender_id, client_message_id, message_type, body)
                        VALUES (:publicId, :meetingId, :senderId, :clientMessageId, :messageType, :body)
                        ON DUPLICATE KEY UPDATE body = body
                        """)
                .param("publicId", publicId)
                .param("meetingId", meetingId)
                .param("senderId", userId)
                .param("clientMessageId", request.clientMessageId())
                .param("messageType", request.messageType() == null ? "TEXT" : request.messageType())
                .param("body", request.body())
                .update();
        ChatDtos.MessageResponse message = jdbc.sql("""
                        SELECT cm.public_id, m.public_id meeting_public_id, u.public_id sender_public_id, cm.body, cm.message_type, cm.created_at
                        FROM chat_messages cm JOIN meetings m ON m.id = cm.meeting_id JOIN users u ON u.id = cm.sender_id
                        WHERE cm.sender_id = :senderId AND cm.client_message_id = :clientMessageId
                        """)
                .param("senderId", userId)
                .param("clientMessageId", request.clientMessageId())
                .query(this::messageRow)
                .single();
        messagingTemplate.convertAndSend("/topic/meetings/" + meetingPublicId + "/messages", message);
        return message;
    }

    public List<ChatDtos.MessageResponse> messages(String userPublicId, String meetingPublicId, Long afterId, int size) {
        long userId = db.userId(userPublicId);
        long meetingId = db.meetingId(meetingPublicId);
        if (!canAccessMeeting(userId, meetingId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "You are not a participant of this meeting");
        }
        return jdbc.sql("""
                        SELECT cm.public_id, m.public_id meeting_public_id, u.public_id sender_public_id, cm.body, cm.message_type, cm.created_at
                        FROM chat_messages cm JOIN meetings m ON m.id = cm.meeting_id JOIN users u ON u.id = cm.sender_id
                        WHERE cm.meeting_id = :meetingId AND cm.deleted_at IS NULL AND cm.id > :afterId
                        ORDER BY cm.id ASC LIMIT :limit
                        """)
                .param("meetingId", meetingId)
                .param("afterId", afterId == null ? 0 : afterId)
                .param("limit", Math.min(size, 100))
                .query(this::messageRow)
                .list();
    }

    @Transactional
    public void read(String userPublicId, String messagePublicId) {
        long userId = db.userId(userPublicId);
        Long messageId = jdbc.sql("SELECT id FROM chat_messages WHERE public_id = :publicId")
                .param("publicId", messagePublicId)
                .query(Long.class)
                .single();
        jdbc.sql("INSERT IGNORE INTO chat_read_receipts(message_id, user_id) VALUES (:messageId, :userId)")
                .param("messageId", messageId)
                .param("userId", userId)
                .update();
    }

    public void typing(String userPublicId, String meetingPublicId) {
        messagingTemplate.convertAndSend("/topic/meetings/" + meetingPublicId + "/typing", userPublicId);
    }

    private boolean canAccessMeeting(long userId, long meetingId) {
        Long count = jdbc.sql("""
                        SELECT COUNT(*) FROM bookings b WHERE b.user_id = :userId AND b.meeting_id = :meetingId AND b.status IN ('CONFIRMED','PAYMENT_PENDING')
                        UNION ALL
                        SELECT COUNT(*) FROM meetings m JOIN consultant_profiles cp ON cp.id = m.consultant_id WHERE m.id = :meetingId AND cp.user_id = :userId
                        """)
                .param("userId", userId)
                .param("meetingId", meetingId)
                .query(Long.class)
                .list()
                .stream()
                .mapToLong(Long::longValue)
                .sum();
        return count > 0;
    }

    private ChatDtos.MessageResponse messageRow(ResultSet rs, int row) throws SQLException {
        return new ChatDtos.MessageResponse(
                rs.getString("public_id"),
                rs.getString("meeting_public_id"),
                rs.getString("sender_public_id"),
                rs.getString("body"),
                rs.getString("message_type"),
                rs.getTimestamp("created_at").toInstant()
        );
    }
}
