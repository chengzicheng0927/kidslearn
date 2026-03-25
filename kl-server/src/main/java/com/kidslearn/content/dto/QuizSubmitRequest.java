package com.kidslearn.content.dto;

import jakarta.validation.constraints.*;
import java.util.UUID;

public record QuizSubmitRequest(
    @NotNull UUID questionId, UUID planQuestionId,
    @NotBlank String answerId, @Min(0) int timeSpent
) {}
