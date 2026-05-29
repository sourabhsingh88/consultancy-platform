package com.consultancy.platform.modules.video;

import com.consultancy.platform.common.config.DatabaseSupport;
import com.consultancy.platform.common.exception.BusinessException;
import io.livekit.server.AccessToken;
import io.livekit.server.RoomJoin;
import io.livekit.server.RoomName;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
public class VideoService {
    private final JdbcClient jdbc;
    private final DatabaseSupport db;
    private final String livekitApiKey;
    private final String livekitApiSecret;

    public VideoService(JdbcClient jdbc, DatabaseSupport db,
                        @Value("${app.video.api-key:default-key}") String livekitApiKey,
                        @Value("${app.video.api-secret:default-secret}") String livekitApiSecret) {
        this.jdbc = jdbc;
        this.db = db;
        this.livekitApiKey = livekitApiKey;
        this.livekitApiSecret = livekitApiSecret;
    }

    public String generateJoinToken(String userPublicId, String meetingPublicId) {
        long userId = db.userId(userPublicId);
        long meetingId = db.meetingId(meetingPublicId);

        // Verify the user is a participant or the consultant
        if (!canAccessMeeting(userId, meetingId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "You do not have access to this meeting");
        }

        String displayName = jdbc.sql("SELECT display_name FROM users WHERE id = :userId")
                .param("userId", userId).query(String.class).single();

        // Generate a secure token scoped to this specific meeting
        AccessToken token = new AccessToken(livekitApiKey, livekitApiSecret);
        token.setName(displayName);
        token.setIdentity(userPublicId);
        token.addGrants(new RoomJoin(true), new RoomName(meetingPublicId));

        return token.toJwt();
    }
    private boolean canAccessMeeting(long userId, long meetingId) {
        return jdbc.sql("""
                SELECT COUNT(*) FROM bookings b WHERE b.user_id = :userId AND b.meeting_id = :meetingId AND b.status IN ('CONFIRMED','LIVE')
                UNION ALL
                SELECT COUNT(*) FROM meetings m JOIN consultant_profiles cp ON cp.id = m.consultant_id WHERE m.id = :meetingId AND cp.user_id = :userId
                """)
                .param("userId", userId).param("meetingId", meetingId)
                .query(Long.class).list().stream().mapToLong(Long::longValue).sum() > 0;
    }
}
