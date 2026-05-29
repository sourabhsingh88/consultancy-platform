package com.consultancy.platform.modules.admin;

import com.consultancy.platform.common.config.DatabaseSupport;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
public class AdminService {
    private final JdbcClient jdbc;
    private final DatabaseSupport db;

    public AdminService(JdbcClient jdbc, DatabaseSupport db) {
        this.jdbc = jdbc;
        this.db = db;
    }

    public List<AdminDtos.UserRow> users(int page, int size) {
        return jdbc.sql("SELECT public_id, email, display_name, status, created_at FROM users ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
                .param("limit", Math.min(size, 100))
                .param("offset", page * size)
                .query(this::userRow)
                .list();
    }

    @Transactional
    public void banUser(String publicId) {
        jdbc.sql("UPDATE users SET status = 'BANNED' WHERE public_id = :publicId").param("publicId", publicId).update();
    }

    public List<AdminDtos.PaymentRow> payments(int page, int size) {
        return jdbc.sql("""
                        SELECT p.public_id, b.public_id booking_public_id, p.provider_order_id, p.amount, p.currency, p.status, p.created_at
                        FROM payments p JOIN bookings b ON b.id = p.booking_id
                        ORDER BY p.created_at DESC LIMIT :limit OFFSET :offset
                        """)
                .param("limit", Math.min(size, 100))
                .param("offset", page * size)
                .query(this::paymentRow)
                .list();
    }

    public AdminDtos.Analytics analytics() {
        long users = count("users");
        long consultants = count("consultant_profiles");
        long meetings = count("meetings");
        long bookings = count("bookings");
        long revenue = jdbc.sql("SELECT COALESCE(SUM(amount), 0) FROM payments WHERE status = 'CAPTURED'").query(Long.class).single();
        return new AdminDtos.Analytics(users, consultants, meetings, bookings, revenue);
    }

    @Transactional
    public void announcement(AdminDtos.AnnouncementRequest request) {
        List<Long> userIds = jdbc.sql("SELECT id FROM users WHERE status = 'ACTIVE'").query(Long.class).list();
        for (Long userId : userIds) {
            jdbc.sql("INSERT INTO notifications(public_id, user_id, type, title, body, data) VALUES (:publicId, :userId, 'ADMIN_ANNOUNCEMENT', :title, :body, JSON_OBJECT())")
                    .param("publicId", db.uuid())
                    .param("userId", userId)
                    .param("title", request.title())
                    .param("body", request.body())
                    .update();
        }
    }

    private long count(String table) {
        return jdbc.sql("SELECT COUNT(*) FROM " + table).query(Long.class).single();
    }

    private AdminDtos.UserRow userRow(ResultSet rs, int row) throws SQLException {
        return new AdminDtos.UserRow(rs.getString("public_id"), rs.getString("email"), rs.getString("display_name"), rs.getString("status"), rs.getTimestamp("created_at").toInstant());
    }

    private AdminDtos.PaymentRow paymentRow(ResultSet rs, int row) throws SQLException {
        return new AdminDtos.PaymentRow(rs.getString("public_id"), rs.getString("booking_public_id"), rs.getString("provider_order_id"), rs.getLong("amount"), rs.getString("currency"), rs.getString("status"), rs.getTimestamp("created_at").toInstant());
    }
}
