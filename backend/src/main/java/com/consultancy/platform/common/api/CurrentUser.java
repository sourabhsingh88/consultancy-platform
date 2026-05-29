package com.consultancy.platform.common.api;

import org.springframework.security.core.Authentication;

public final class CurrentUser {
    private CurrentUser() {
    }

    public static String publicId(Authentication authentication) {
        return authentication.getName();
    }
}
