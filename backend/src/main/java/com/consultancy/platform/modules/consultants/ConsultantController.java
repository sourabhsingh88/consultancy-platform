package com.consultancy.platform.modules.consultants;

import com.consultancy.platform.common.api.ApiResponse;
import com.consultancy.platform.common.api.CurrentUser;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class ConsultantController {
    private final ConsultantService consultantService;

    public ConsultantController(ConsultantService consultantService) {
        this.consultantService = consultantService;
    }

    @PostMapping("/consultants/me/profile")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ConsultantDtos.ConsultantResponse> upsertProfile(@Valid @RequestBody ConsultantDtos.UpsertProfileRequest request,
                                                                         Authentication authentication) {
        return ApiResponse.ok(consultantService.upsertProfile(CurrentUser.publicId(authentication), request), "Consultant profile saved", MDC.get("traceId"));
    }

    @GetMapping("/consultants")
    public ApiResponse<List<ConsultantDtos.ConsultantResponse>> consultants(@RequestParam(defaultValue = "0") int page,
                                                                            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(consultantService.listConsultants(page, Math.min(size, 100)), "OK", MDC.get("traceId"));
    }

    @GetMapping("/consultants/{consultantPublicId}")
    public ApiResponse<ConsultantDtos.ConsultantResponse> consultant(@PathVariable String consultantPublicId) {
        return ApiResponse.ok(consultantService.profile(consultantPublicId), "OK", MDC.get("traceId"));
    }

    @PostMapping("/consultants/me/availability-rules")
    @PreAuthorize("hasRole('CONSULTANT')")
    public ApiResponse<Map<String, String>> availability(@Valid @RequestBody ConsultantDtos.AvailabilityRuleRequest request,
                                                         Authentication authentication) {
        String publicId = consultantService.addAvailability(CurrentUser.publicId(authentication), request);
        return ApiResponse.ok(Map.of("publicId", publicId), "Availability rule created", MDC.get("traceId"));
    }

    @GetMapping("/consultants/{consultantPublicId}/slots")
    public ApiResponse<List<ConsultantDtos.SlotResponse>> slots(@PathVariable String consultantPublicId,
                                                                @RequestParam Instant from,
                                                                @RequestParam Instant to) {
        return ApiResponse.ok(consultantService.availableSlots(consultantPublicId, from, to), "OK", MDC.get("traceId"));
    }

    @PostMapping("/seminars")
    @PreAuthorize("hasRole('CONSULTANT')")
    public ApiResponse<ConsultantDtos.MeetingResponse> createSeminar(@Valid @RequestBody ConsultantDtos.SeminarRequest request,
                                                                     Authentication authentication) {
        return ApiResponse.ok(consultantService.createSeminar(CurrentUser.publicId(authentication), request), "Seminar created", MDC.get("traceId"));
    }

    @GetMapping("/seminars")
    public ApiResponse<List<ConsultantDtos.MeetingResponse>> seminars(@RequestParam(defaultValue = "0") int page,
                                                                      @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(consultantService.listSeminars(page, Math.min(size, 100)), "OK", MDC.get("traceId"));
    }
}
