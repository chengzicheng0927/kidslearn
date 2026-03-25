package com.kidslearn.user.dto;

import java.util.UUID;

public record ChildDTO(
    UUID id,
    String name,
    int age,
    String ageGroup,
    int stars,
    int streak,
    int todayMinutes,
    int level,
    int xp,
    int xpNeeded,
    int totalAnswered,
    int totalDays,
    int dailyTargetEn,
    int dailyTargetMa
) {}
