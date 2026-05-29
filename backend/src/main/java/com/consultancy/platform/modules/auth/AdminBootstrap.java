package com.consultancy.platform.modules.auth;

import com.consultancy.platform.common.config.DatabaseSupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminBootstrap implements ApplicationRunner {
    private final JdbcClient jdbc;
    private final DatabaseSupport db;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String displayName;

    public AdminBootstrap(JdbcClient jdbc, DatabaseSupport db, PasswordEncoder passwordEncoder,
                          @Value("${app.admin-bootstrap.email}") String email,
                          @Value("${app.admin-bootstrap.password}") String password,
                          @Value("${app.admin-bootstrap.display-name}") String displayName) {
        this.jdbc = jdbc;
        this.db = db;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
        this.displayName = displayName;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return;
        }
        long userId = db.userIdByEmail(email).orElseGet(() -> {
            String publicId = db.uuid();
            jdbc.sql("INSERT INTO users(public_id, email, display_name, auth_type, email_verified) VALUES (:publicId, :email, :displayName, 'ADMIN', TRUE)")
                    .param("publicId", publicId)
                    .param("email", email)
                    .param("displayName", displayName)
                    .update();
            return db.userIdByEmail(email).orElseThrow();
        });
        db.grantRole(userId, "SUPER_ADMIN");
        jdbc.sql("""
                        INSERT INTO admin_credentials(user_id, password_hash)
                        VALUES (:userId, :hash)
                        ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash)
                        """)
                .param("userId", userId)
                .param("hash", passwordEncoder.encode(password))
                .update();
    }
}
