package com.kidslearn.llm.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public record LlmResponse(
    String id,
    String type,
    String role,
    List<ContentBlock> content,
    String model,
    String stopReason
) {
    public record ContentBlock(
        String type,
        String text,
        String name,
        JsonNode input,
        String toolUseId
    ) {}
}
