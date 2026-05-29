package com.consultancy.platform.common.config;

import com.consultancy.platform.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class DatabaseSupport {
    private final JdbcClient jdbc;

    public DatabaseSupport(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public String uuid() {
        return UUID.randomUUID().toString();
    }

    public long userId(String publicId) {
        return jdbc.sql("SELECT id FROM users WHERE public_id = :publicId AND deleted_at IS NULL")
                .param("publicId", publicId)
                .query(Long.class)
                .optional()
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public long consultantId(String publicId) {
        return jdbc.sql("SELECT id FROM consultant_profiles WHERE public_id = :publicId AND deleted_at IS NULL")
                .param("publicId", publicId)
                .query(Long.class)
                .optional()
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Consultant not found"));
    }

    public long meetingId(String publicId) {
        return jdbc.sql("SELECT id FROM meetings WHERE public_id = :publicId AND deleted_at IS NULL")
                .param("publicId", publicId)
                .query(Long.class)
                .optional()
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Meeting not found"));
    }

    public long roleId(String role) {
        return jdbc.sql("SELECT id FROM roles WHERE name = :role").param("role", role).query(Long.class).single();
    }

    public void grantRole(long userId, String role) {
        jdbc.sql("INSERT IGNORE INTO user_roles(user_id, role_id) VALUES (:userId, :roleId)")
                .param("userId", userId)
                .param("roleId", roleId(role))
                .update();
    }

    public List<String> roles(long userId) {
        return jdbc.sql("""
                        SELECT r.name FROM roles r
                        JOIN user_roles ur ON ur.role_id = r.id
                        WHERE ur.user_id = :userId
                        """)
                .param("userId", userId)
                .query(String.class)
                .list();
    }

    public Optional<Long> userIdByEmail(String email) {
        return jdbc.sql("SELECT id FROM users WHERE email = :email AND deleted_at IS NULL")
                .param("email", email)
                .query(Long.class)
                .optional();
    }

    public String userPublicId(long userId) {
        return jdbc.sql("SELECT public_id FROM users WHERE id = :userId")
                .param("userId", userId)
                .query(String.class)
                .single();
    }
}
