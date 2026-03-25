package com.kidslearn.user.dto;

import jakarta.validation.constraints.NotBlank;

public record ValidateIdCardRequest(
    @NotBlank String idCard
) {}
