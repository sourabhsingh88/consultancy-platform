package com.consultancy.platform.modules.ai;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public final class AiDtos {
    private AiDtos() {
    }

    public record SummaryResponse(String publicId, String status, String summary, List<String> keyPoints,
                                  List<String> actionItems, String followUpNotes) {
    }

    public record SupportQuestionRequest(@NotBlank String question) {
    }

    public record SupportAnswerResponse(String answer, boolean contextUsed) {
    }
}
