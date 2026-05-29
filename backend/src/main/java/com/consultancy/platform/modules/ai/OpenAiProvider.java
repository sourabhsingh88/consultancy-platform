package com.consultancy.platform.modules.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "openai")
public class OpenAiProvider implements AiProvider {
    private final RestClient restClient = RestClient.create("https://api.openai.com/v1");
    private final String apiKey;
    private final String model;

    public OpenAiProvider(@Value("${app.ai.openai-api-key}") String apiKey, @Value("${app.ai.openai-model}") String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public AiDtos.SummaryResponse summarize(String meetingTitle, String context) {
        String content = complete("Create a concise meeting summary with key points, action items, and follow-up notes.\nTitle: " + meetingTitle + "\nContext:\n" + context);
        return new AiDtos.SummaryResponse(null, "COMPLETED", content, List.of(), List.of(), "");
    }

    @Override
    public AiDtos.SupportAnswerResponse answer(String question, String allowedContext) {
        String content = complete("Answer only from this meeting context. If not enough context, say so.\nContext:\n" + allowedContext + "\nQuestion:\n" + question);
        return new AiDtos.SupportAnswerResponse(content, true);
    }

    private String complete(String prompt) {
        Map<?, ?> response = restClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .body(Map.of(
                        "model", model,
                        "messages", List.of(Map.of("role", "user", "content", prompt)),
                        "temperature", 0.2
                ))
                .retrieve()
                .body(Map.class);
        List<?> choices = (List<?>) response.get("choices");
        Map<?, ?> choice = (Map<?, ?>) choices.getFirst();
        Map<?, ?> message = (Map<?, ?>) choice.get("message");
        return String.valueOf(message.get("content"));
    }
}
