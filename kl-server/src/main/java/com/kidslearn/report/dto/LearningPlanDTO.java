package com.kidslearn.report.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record LearningPlanDTO(
    UUID id, String subject, int duration, String dailyGoal,
    List<PlanPhaseDTO> phases, String difficulty, int estimatedMinutes, String status,
    LocalDate createdAt, LocalDate startedAt, LocalDate endedAt,
    int currentPhaseIndex
) {
    public record PlanPhaseDTO(
        String dayRange, String goal, String topic,
        int questionsPerDay, String difficulty,
        List<QuestionDTO> questions
    ) {}

    public record QuestionDTO(
        UUID id, String icon, String question,
        List<String> options, String answer
    ) {}
}
