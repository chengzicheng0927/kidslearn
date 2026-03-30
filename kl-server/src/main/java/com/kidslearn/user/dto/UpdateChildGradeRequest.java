package com.kidslearn.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateChildGradeRequest(
    @NotBlank(message = "年级不能为空")
    String grade
) {}
