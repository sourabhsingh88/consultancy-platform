package com.consultancy.platform.modules.ai;

import com.consultancy.platform.common.api.ApiResponse;
import com.consultancy.platform.common.api.CurrentUser;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/meetings/{meetingPublicId}")
public class AiController {
    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/summary/generate")
    public ApiResponse<AiDtos.SummaryResponse> generate(@PathVariable String meetingPublicId, Authentication authentication) {
        return ApiResponse.ok(aiService.generateSummary(CurrentUser.publicId(authentication), meetingPublicId), "Summary generated", MDC.get("traceId"));
    }

    @GetMapping("/summary")
    public ApiResponse<AiDtos.SummaryResponse> summary(@PathVariable String meetingPublicId, Authentication authentication) {
        return ApiResponse.ok(aiService.latestSummary(CurrentUser.publicId(authentication), meetingPublicId), "OK", MDC.get("traceId"));
    }

    @PostMapping("/ai-support/messages")
    public ApiResponse<AiDtos.SupportAnswerResponse> support(@PathVariable String meetingPublicId,
                                                             @Valid @RequestBody AiDtos.SupportQuestionRequest request,
                                                             Authentication authentication) {
        return ApiResponse.ok(aiService.answer(CurrentUser.publicId(authentication), meetingPublicId, request), "OK", MDC.get("traceId"));
    }
}
