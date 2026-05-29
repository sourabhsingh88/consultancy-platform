package com.consultancy.platform.modules.video;

import com.consultancy.platform.common.api.ApiResponse;
import com.consultancy.platform.common.api.CurrentUser;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/meetings")
public class VideoController {
    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @GetMapping("/{meetingPublicId}/video-token")
    public ApiResponse<String> getVideoToken(@PathVariable String meetingPublicId, Authentication authentication) {
        String token = videoService.generateJoinToken(CurrentUser.publicId(authentication), meetingPublicId);
        return ApiResponse.ok(token, "Video access token generated", MDC.get("traceId"));
    }
}
