package com.consultancy.platform.modules.admin;

import com.consultancy.platform.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public ApiResponse<List<AdminDtos.UserRow>> users(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(adminService.users(page, size), "OK", MDC.get("traceId"));
    }

    @PatchMapping("/users/{userPublicId}/ban")
    public ApiResponse<Void> ban(@PathVariable String userPublicId) {
        adminService.banUser(userPublicId);
        return ApiResponse.ok(null, "User banned", MDC.get("traceId"));
    }

    @GetMapping("/payments")
    public ApiResponse<List<AdminDtos.PaymentRow>> payments(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(adminService.payments(page, size), "OK", MDC.get("traceId"));
    }

    @GetMapping("/analytics/overview")
    public ApiResponse<AdminDtos.Analytics> analytics() {
        return ApiResponse.ok(adminService.analytics(), "OK", MDC.get("traceId"));
    }

    @PostMapping("/announcements")
    public ApiResponse<Void> announcement(@Valid @RequestBody AdminDtos.AnnouncementRequest request) {
        adminService.announcement(request);
        return ApiResponse.ok(null, "Announcement sent", MDC.get("traceId"));
    }
}
