package com.kidslearn.user.dto;

public record ValidateIdCardResponse(
    boolean valid,
    Integer age,
    String ageGroup,
    String message
) {}
