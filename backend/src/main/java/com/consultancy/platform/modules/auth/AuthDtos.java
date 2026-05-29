package com.consultancy.platform.modules.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public final class AuthDtos {
    private AuthDtos() {
    }

    public enum Provider {
        GOOGLE, APPLE, FACEBOOK
    }

    public record OAuthLoginRequest(@NotNull Provider provider, @NotBlank String providerToken, @NotBlank String deviceId, String fcmToken) {
    }

    public record AdminLoginRequest(@Email @NotBlank String email, @NotBlank String password, @NotBlank String deviceId) {
    }

    public record RefreshRequest(@NotBlank String refreshToken, @NotBlank String deviceId) {
    }

    public record TokenResponse(String accessToken, String refreshToken, long expiresIn, UserResponse user) {
    }

    public record UserResponse(String publicId, String displayName, List<String> roles) {
    }
}
