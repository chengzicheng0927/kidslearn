package com.kidslearn.content.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WrongQuestionDTO(
    UUID id,
    UUID questionId,
    String subject,
    String topic,
    String questionText,
    List<QuizOption> options,
    String wrongAnswerId,
    String correctAnswerId,
    String explanation,
    Instant answeredAt,
    int wrongCount,
    int correctCount,
    int threshold
) {
    public record QuizOption(String id, String text, String emoji) {}
}
