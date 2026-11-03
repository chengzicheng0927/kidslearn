package com.kidslearn.chat.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record ParentChatRequest(
    @NotBlank String message, UUID planId, String conversationId, ParentContext context
) {
    public record ParentContext(UUID childId, int childAge, String subject) {}
}
