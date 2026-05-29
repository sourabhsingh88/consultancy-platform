package com.consultancy.platform.modules.notifications;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public final class NotificationDtos {
    private NotificationDtos() {
    }

    public record FcmTokenRequest(@NotBlank String token, String deviceId) {
    }

    public record NotificationResponse(String publicId, String type, String title, String body, boolean read, Instant createdAt) {
    }
}
