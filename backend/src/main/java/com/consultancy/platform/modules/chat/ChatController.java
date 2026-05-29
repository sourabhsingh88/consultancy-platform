package com.consultancy.platform.modules.chat;

import com.consultancy.platform.common.api.ApiResponse;
import com.consultancy.platform.common.api.CurrentUser;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ChatController {
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @MessageMapping("/meetings/{meetingPublicId}/chat.send")
    public void sendWs(@DestinationVariable String meetingPublicId, @Valid @Payload ChatDtos.SendMessageRequest request, Principal principal) {
        chatService.send(principal.getName(), meetingPublicId, request);
    }

    @MessageMapping("/meetings/{meetingPublicId}/typing")
    public void typingWs(@DestinationVariable String meetingPublicId, Principal principal) {
        chatService.typing(principal.getName(), meetingPublicId);
    }

    @GetMapping("/meetings/{meetingPublicId}/messages")
    public ApiResponse<List<ChatDtos.MessageResponse>> messages(@PathVariable String meetingPublicId,
                                                                @RequestParam(required = false) Long afterMessageId,
                                                                @RequestParam(defaultValue = "50") int size,
                                                                Authentication authentication) {
        return ApiResponse.ok(chatService.messages(CurrentUser.publicId(authentication), meetingPublicId, afterMessageId, size), "OK", MDC.get("traceId"));
    }

    @PostMapping("/messages/{messagePublicId}/read")
    public ApiResponse<Void> read(@PathVariable String messagePublicId, Authentication authentication) {
        chatService.read(CurrentUser.publicId(authentication), messagePublicId);
        return ApiResponse.ok(null, "Read receipt saved", MDC.get("traceId"));
    }
}
