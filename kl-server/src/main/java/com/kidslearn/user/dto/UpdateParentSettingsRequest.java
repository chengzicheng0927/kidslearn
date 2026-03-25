package com.kidslearn.user.dto;

import java.time.LocalTime;

public record UpdateParentSettingsRequest(
    Integer dailyLimitMinutes, Boolean dndEnabled,
    LocalTime dndStart, LocalTime dndEnd,
    Boolean eyeCareReminder, Boolean breakReminder, String teacherName
) {}
