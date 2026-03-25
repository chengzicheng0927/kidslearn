package com.kidslearn.user.dto;

import java.time.Instant;

public record VerifyPasswordResponse(boolean success, int attemptsLeft, Instant lockedUntil) {}
