package com.kidslearn.user.dto;

public record CheckUserStatusResponse(
    String token,
    ChildDTO child,
    ParentSettingsDTO parentSettings
) {}
