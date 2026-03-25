package com.kidslearn.llm.tools;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.UUID;

/**
 * JSON Tool 接口 - LLM Function Calling 工具
 */
public interface JsonTool {

    /** 工具名称，LLM 通过此名称调用 */
    String name();

    /** 工具描述 */
    String description();

    /** 输入参数 JSON Schema */
    JsonNode inputSchema();

    /** 执行工具逻辑 */
    JsonNode execute(JsonNode input, ToolContext ctx);
}
