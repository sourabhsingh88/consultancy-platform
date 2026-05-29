package com.consultancy.platform.modules.ai;

public interface AiProvider {
    AiDtos.SummaryResponse summarize(String meetingTitle, String context);

    AiDtos.SupportAnswerResponse answer(String question, String allowedContext);
}
