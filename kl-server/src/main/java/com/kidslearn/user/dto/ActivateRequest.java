package com.kidslearn.user.dto;

import jakarta.validation.constraints.NotBlank;

public record ActivateRequest(
    @NotBlank String inviteCode,
    @NotBlank String idCard,
    @NotBlank String name,
    String teacherName,
    @NotBlank String deviceId
) {}
