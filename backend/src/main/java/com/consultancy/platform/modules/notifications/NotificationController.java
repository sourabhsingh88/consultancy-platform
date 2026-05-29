package com.consultancy.platform.modules.notifications;

import com.consultancy.platform.common.api.ApiResponse;
import com.consultancy.platform.common.api.CurrentUser;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/devices/fcm-token")
    public ApiResponse<Void> fcm(@Valid @RequestBody NotificationDtos.FcmTokenRequest request, Authentication authentication) {
        notificationService.registerFcm(CurrentUser.publicId(authentication), request);
        return ApiResponse.ok(null, "FCM token saved", MDC.get("traceId"));
    }

    @GetMapping("/notifications")
    public ApiResponse<List<NotificationDtos.NotificationResponse>> list(@RequestParam(defaultValue = "0") int page,
                                                                         @RequestParam(defaultValue = "20") int size,
                                                                         Authentication authentication) {
        return ApiResponse.ok(notificationService.list(CurrentUser.publicId(authentication), page, size), "OK", MDC.get("traceId"));
    }

    @PostMapping("/notifications/{notificationPublicId}/read")
    public ApiResponse<Void> read(@PathVariable String notificationPublicId, Authentication authentication) {
        notificationService.markRead(CurrentUser.publicId(authentication), notificationPublicId);
        return ApiResponse.ok(null, "Notification marked read", MDC.get("traceId"));
    }
}
