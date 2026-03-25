package com.kidslearn.user.dto;

import jakarta.validation.constraints.NotBlank;

public record DeviceRegisterRequest(@NotBlank String deviceId, String pushToken, String platform) {}
