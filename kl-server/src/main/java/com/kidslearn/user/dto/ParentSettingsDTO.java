package com.kidslearn.user.dto;

import java.time.LocalTime;
import java.util.UUID;

public record ParentSettingsDTO(
    int dailyLimitMinutes,
    boolean dndEnabled,
    LocalTime dndStart,
    LocalTime dndEnd,
    boolean eyeCareReminder,
    boolean breakReminder,
    String teacherName
) {}
