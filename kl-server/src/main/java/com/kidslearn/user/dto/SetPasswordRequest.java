package com.kidslearn.user.dto;

import jakarta.validation.constraints.Pattern;

public record SetPasswordRequest(@Pattern(regexp = "\\d{6}") String password) {}
