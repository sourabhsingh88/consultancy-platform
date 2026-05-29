package com.consultancy.platform.modules.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class ChatDtos {
    private ChatDtos() {
    }

    public record SendMessageRequest(@NotBlank String clientMessageId, @NotBlank @Size(max = 4000) String body,
                                     String messageType) {
    }

    public record MessageResponse(String publicId, String meetingPublicId, String senderPublicId, String body,
                                  String messageType, Instant createdAt) {
    }
}
