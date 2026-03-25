package com.kidslearn.content.dto;

import java.util.List;
import java.util.UUID;

public record QuestionDTO(
    UUID id, UUID planQuestionId, String purpose, String subject,
    String label, String question, List<QuizOption> options, int estimatedMinutes
) {
    public record QuizOption(String id, String text, String emoji) {}
}
