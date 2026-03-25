package com.kidslearn.user.dto;

import jakarta.validation.constraints.*;

public record RegisterChildRequest(
    @NotBlank String name,
    @Min(3) @Max(14) int age,
    @NotBlank String ageGroup,
    @NotBlank String deviceId
) {}
