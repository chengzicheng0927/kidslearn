package com.kidslearn.report.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * 计划详情 DTO
 */
public record PlanDetailDTO(
    UUID id,
    String subject,
    String status,
    String difficulty,
    int durationDays,
    String dailyGoal,
    List<PlanPhaseDetailDTO> phases,
    int estimatedMinutes,
    LocalDate startedAt,
    LocalDate completedAt,
    int totalCompleted,
    int totalCorrect,
    double accuracy,
    LocalDate createdAt
) {

    /**
     * 阶段详情 DTO
     */
    public record PlanPhaseDetailDTO(
        int phaseIndex,
        String dayRange,
        String goal,
        String topic,
        int questionsPerDay,
        String difficulty,
        List<LearningPlanDTO.QuestionDTO> questions
    ) {}
}
