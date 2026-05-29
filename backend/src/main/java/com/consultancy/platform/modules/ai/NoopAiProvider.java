package com.consultancy.platform.modules.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "noop", matchIfMissing = true)
public class NoopAiProvider implements AiProvider {
    @Override
    public AiDtos.SummaryResponse summarize(String meetingTitle, String context) {
        String summary = context == null || context.isBlank()
                ? "No transcript or chat context was available for this meeting."
                : "Summary generated from available meeting chat context.";
        return new AiDtos.SummaryResponse(null, "COMPLETED", summary, List.of("Review the meeting chat history"), List.of(), "Configure OPENAI_API_KEY for richer summaries.");
    }

    @Override
    public AiDtos.SupportAnswerResponse answer(String question, String allowedContext) {
        return new AiDtos.SupportAnswerResponse("AI provider is not configured. Available context length: " + (allowedContext == null ? 0 : allowedContext.length()), false);
    }
}
