package com.kidslearn.report.dto;

public record CalendarStatsDTO(
    int totalQuestions, int currentStreak, int bestStreak,
    int todayCorrect, int todayTotal, int totalStars
) {}
