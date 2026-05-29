package com.consultancy.platform.modules.notifications;

import com.consultancy.platform.common.config.DatabaseSupport;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
public class NotificationService {
    private final JdbcClient jdbc;
    private final DatabaseSupport db;

    public NotificationService(JdbcClient jdbc, DatabaseSupport db) {
        this.jdbc = jdbc;
        this.db = db;
    }

    @Transactional
    public void registerFcm(String userPublicId, NotificationDtos.FcmTokenRequest request) {
        long userId = db.userId(userPublicId);
        Long sessionId = request.deviceId() == null ? null : jdbc.sql("SELECT id FROM device_sessions WHERE user_id = :userId AND device_id = :deviceId")
                .param("userId", userId)
                .param("deviceId", request.deviceId())
                .query(Long.class)
                .optional()
                .orElse(null);
        jdbc.sql("""
                        INSERT INTO fcm_tokens(user_id, device_session_id, token, active)
                        VALUES (:userId, :sessionId, :token, TRUE)
                        ON DUPLICATE KEY UPDATE user_id = VALUES(user_id), device_session_id = VALUES(device_session_id), active = TRUE
                        """)
                .param("userId", userId)
                .param("sessionId", sessionId)
                .param("token", request.token())
                .update();
    }

    @Async
    @Transactional
    public void create(String userPublicId, String type, String title, String body, String dataJson) {
        long userId = db.userId(userPublicId);
        jdbc.sql("INSERT INTO notifications(public_id, user_id, type, title, body, data) VALUES (:publicId, :userId, :type, :title, :body, CAST(:data AS JSON))")
                .param("publicId", db.uuid())
                .param("userId", userId)
                .param("type", type)
                .param("title", title)
                .param("body", body)
                .param("data", dataJson == null ? "{}" : dataJson)
                .update();
        if (!FirebaseApp.getApps().isEmpty()) {
            List<String> tokens = jdbc.sql("SELECT token FROM fcm_tokens WHERE user_id = :userId AND active = TRUE")
                    .param("userId", userId)
                    .query(String.class)
                    .list();
            for (String token : tokens) {
                try {
                    Message message = Message.builder()
                            .setToken(token)
                            .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                            .putData("type", type)
                            .build();
                    FirebaseMessaging.getInstance().send(message);
                } catch (Exception ignored) {
                    jdbc.sql("UPDATE fcm_tokens SET active = FALSE WHERE token = :token").param("token", token).update();
                }
            }
        }
    }

    public List<NotificationDtos.NotificationResponse> list(String userPublicId, int page, int size) {
        long userId = db.userId(userPublicId);
        return jdbc.sql("""
                        SELECT public_id, type, title, body, read_at, created_at
                        FROM notifications WHERE user_id = :userId
                        ORDER BY created_at DESC LIMIT :limit OFFSET :offset
                        """)
                .param("userId", userId)
                .param("limit", Math.min(size, 100))
                .param("offset", page * size)
                .query(this::row)
                .list();
    }

    @Transactional
    public void markRead(String userPublicId, String notificationPublicId) {
        long userId = db.userId(userPublicId);
        jdbc.sql("UPDATE notifications SET read_at = UTC_TIMESTAMP(6) WHERE user_id = :userId AND public_id = :publicId")
                .param("userId", userId)
                .param("publicId", notificationPublicId)
                .update();
    }

    private NotificationDtos.NotificationResponse row(ResultSet rs, int row) throws SQLException {
        return new NotificationDtos.NotificationResponse(
                rs.getString("public_id"),
                rs.getString("type"),
                rs.getString("title"),
                rs.getString("body"),
                rs.getTimestamp("read_at") != null,
                rs.getTimestamp("created_at").toInstant()
        );
    }
}
