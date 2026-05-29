package com.consultancy.platform.modules.auth;

import com.consultancy.platform.common.config.DatabaseSupport;
import com.consultancy.platform.common.exception.BusinessException;
import com.consultancy.platform.common.security.HashingService;
import com.consultancy.platform.common.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;

@Service
public class AuthService {
    private final JdbcClient jdbc;
    private final DatabaseSupport db;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final HashingService hashingService;
    private final OAuthTokenVerifier oauthTokenVerifier;
    private final long refreshDays;

    public AuthService(JdbcClient jdbc, DatabaseSupport db, JwtService jwtService, PasswordEncoder passwordEncoder,
                       HashingService hashingService, OAuthTokenVerifier oauthTokenVerifier,
                       @Value("${app.jwt.refresh-days}") long refreshDays) {
        this.jdbc = jdbc;
        this.db = db;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.hashingService = hashingService;
        this.oauthTokenVerifier = oauthTokenVerifier;
        this.refreshDays = refreshDays;
    }

    @Transactional
    public AuthDtos.TokenResponse oauthLogin(AuthDtos.OAuthLoginRequest request) {
        OAuthTokenVerifier.VerifiedOAuthUser verified = oauthTokenVerifier.verify(request.provider(), request.providerToken());
        long userId = jdbc.sql("""
                        SELECT u.id FROM users u
                        JOIN oauth_accounts oa ON oa.user_id = u.id
                        WHERE oa.provider = :provider AND oa.provider_subject = :subject
                        """)
                .param("provider", request.provider().name())
                .param("subject", verified.providerSubject())
                .query(Long.class)
                .optional()
                .orElseGet(() -> createOAuthUser(request, verified));
        db.grantRole(userId, "USER");
        return issueSession(userId, request.deviceId(), request.fcmToken());
    }

    @Transactional
    public AuthDtos.TokenResponse adminLogin(AuthDtos.AdminLoginRequest request) {
        long userId = db.userIdByEmail(request.email())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        String hash = jdbc.sql("SELECT password_hash FROM admin_credentials WHERE user_id = :userId")
                .param("userId", userId)
                .query(String.class)
                .optional()
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), hash)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return issueSession(userId, request.deviceId(), null);
    }

    @Transactional
    public AuthDtos.TokenResponse refresh(AuthDtos.RefreshRequest request) {
        String refreshHash = hashingService.sha256(request.refreshToken());
        long userId = jdbc.sql("""
                        SELECT user_id FROM device_sessions
                        WHERE device_id = :deviceId AND refresh_token_hash = :hash
                          AND revoked_at IS NULL AND expires_at > UTC_TIMESTAMP(6)
                        """)
                .param("deviceId", request.deviceId())
                .param("hash", refreshHash)
                .query(Long.class)
                .optional()
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        return issueSession(userId, request.deviceId(), null);
    }

    @Transactional
    public void logout(String refreshToken) {
        jdbc.sql("UPDATE device_sessions SET revoked_at = UTC_TIMESTAMP(6) WHERE refresh_token_hash = :hash")
                .param("hash", hashingService.sha256(refreshToken))
                .update();
    }

    private long createOAuthUser(AuthDtos.OAuthLoginRequest request, OAuthTokenVerifier.VerifiedOAuthUser verified) {
        String publicId = db.uuid();
        jdbc.sql("""
                        INSERT INTO users(public_id, email, display_name, avatar_url, auth_type, email_verified)
                        VALUES (:publicId, :email, :displayName, :avatarUrl, 'OAUTH', :emailVerified)
                        """)
                .param("publicId", publicId)
                .param("email", verified.email())
                .param("displayName", verified.displayName())
                .param("avatarUrl", verified.avatarUrl())
                .param("emailVerified", verified.emailVerified())
                .update();
        long userId = jdbc.sql("SELECT id FROM users WHERE public_id = :publicId")
                .param("publicId", publicId)
                .query(Long.class)
                .single();
        jdbc.sql("""
                        INSERT INTO oauth_accounts(user_id, provider, provider_subject, provider_email)
                        VALUES (:userId, :provider, :subject, :email)
                        """)
                .param("userId", userId)
                .param("provider", request.provider().name())
                .param("subject", verified.providerSubject())
                .param("email", verified.email())
                .update();
        return userId;
    }

    private AuthDtos.TokenResponse issueSession(long userId, String deviceId, String fcmToken) {
        String refreshToken = newRefreshToken();
        String refreshHash = hashingService.sha256(refreshToken);
        Instant expiresAt = Instant.now().plus(refreshDays, ChronoUnit.DAYS);
        jdbc.sql("""
                        INSERT INTO device_sessions(public_id, user_id, device_id, refresh_token_hash, expires_at, last_seen_at)
                        VALUES (:publicId, :userId, :deviceId, :hash, :expiresAt, UTC_TIMESTAMP(6))
                        ON DUPLICATE KEY UPDATE refresh_token_hash = VALUES(refresh_token_hash),
                          revoked_at = NULL, expires_at = VALUES(expires_at), last_seen_at = UTC_TIMESTAMP(6)
                        """)
                .param("publicId", db.uuid())
                .param("userId", userId)
                .param("deviceId", deviceId)
                .param("hash", refreshHash)
                .param("expiresAt", expiresAt)
                .update();
        if (fcmToken != null && !fcmToken.isBlank()) {
            Long sessionId = jdbc.sql("SELECT id FROM device_sessions WHERE user_id = :userId AND device_id = :deviceId")
                    .param("userId", userId)
                    .param("deviceId", deviceId)
                    .query(Long.class)
                    .single();
            jdbc.sql("""
                            INSERT INTO fcm_tokens(user_id, device_session_id, token, active)
                            VALUES (:userId, :sessionId, :token, TRUE)
                            ON DUPLICATE KEY UPDATE user_id = VALUES(user_id), device_session_id = VALUES(device_session_id), active = TRUE
                            """)
                    .param("userId", userId)
                    .param("sessionId", sessionId)
                    .param("token", fcmToken)
                    .update();
        }
        List<String> roles = db.roles(userId);
        String userPublicId = db.userPublicId(userId);
        String accessToken = jwtService.issueAccessToken(userPublicId, roles, deviceId);
        String displayName = jdbc.sql("SELECT display_name FROM users WHERE id = :userId").param("userId", userId).query(String.class).single();
        return new AuthDtos.TokenResponse(accessToken, refreshToken, jwtService.accessSeconds(), new AuthDtos.UserResponse(userPublicId, displayName, roles));
    }

    private String newRefreshToken() {
        byte[] bytes = new byte[48];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
