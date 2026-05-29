package com.consultancy.platform.modules.auth;

import com.consultancy.platform.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/oauth/login")
    public ApiResponse<AuthDtos.TokenResponse> oauthLogin(@Valid @RequestBody AuthDtos.OAuthLoginRequest request) {
        return ApiResponse.ok(authService.oauthLogin(request), "Authenticated", MDC.get("traceId"));
    }

    @PostMapping("/admin/login")
    public ApiResponse<AuthDtos.TokenResponse> adminLogin(@Valid @RequestBody AuthDtos.AdminLoginRequest request) {
        return ApiResponse.ok(authService.adminLogin(request), "Authenticated", MDC.get("traceId"));
    }

    @PostMapping("/token/refresh")
    public ApiResponse<AuthDtos.TokenResponse> refresh(@Valid @RequestBody AuthDtos.RefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request), "Token refreshed", MDC.get("traceId"));
    }
}
