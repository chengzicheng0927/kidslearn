package com.kidslearn.content.dto;

import java.util.List;

/**
 * 成就系统响应
 */
public record AchievementDTO(
    List<AchievementItem> achievements,
    List<String> earnedIds,
    AchievementStats stats
) {
    public record AchievementItem(
        String id,
        String category,
        String icon,
        String title,
        String description,
        int requirement,
        String type
    ) {}

    public record AchievementStats(
        int totalQuestions,
        int currentStreak,
        int bestStreak,
        int todayCorrect,
        int todayTotal
    ) {}
}
