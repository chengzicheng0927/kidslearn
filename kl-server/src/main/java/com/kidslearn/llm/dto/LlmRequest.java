package com.kidslearn.llm.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public record LlmRequest(
    String model,
    List<Message> messages,
    int maxTokens,
    List<ToolChoice> tools
) {
    public record Message(String role, String content) {}
    public record ToolChoice(String name, String description, JsonNode inputSchema) {}
}
