package com.kidslearn.chat.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record ParentChatRequest(
    @NotBlank String message, UUID planId, ParentContext context
) {
    public record ParentContext(UUID childId, int childAge, String subject) {}
}
