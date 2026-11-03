package com.kidslearn.report.dto;

public record CalendarStatsDTO(
    int totalDays, int totalMinutes, int totalQuestions,
    int totalStars, int currentStreak, int longestStreak
) {}
