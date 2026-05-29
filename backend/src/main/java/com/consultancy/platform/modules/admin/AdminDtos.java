package com.consultancy.platform.modules.admin;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public final class AdminDtos {
    private AdminDtos() {
    }

    public record UserRow(String publicId, String email, String displayName, String status, Instant createdAt) {
    }

    public record PaymentRow(String publicId, String bookingPublicId, String providerOrderId, long amount, String currency, String status, Instant createdAt) {
    }

    public record Analytics(long users, long consultants, long meetings, long bookings, long capturedRevenue) {
    }

    public record AnnouncementRequest(@NotBlank String title, @NotBlank String body) {
    }
}
