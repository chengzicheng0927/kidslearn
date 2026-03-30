package com.kidslearn.llm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidslearn.chat.dto.ParentChatRequest;
import com.kidslearn.chat.dto.ParentChatResponse;
import com.kidslearn.llm.LlmPromptTemplate;
import com.kidslearn.llm.dto.LlmRequest;
import com.kidslearn.llm.dto.LlmResponse;
import com.kidslearn.llm.tools.JsonTool;
import com.kidslearn.llm.tools.ToolContext;
import com.kidslearn.llm.tools.registry.ToolRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LlmService {

    private final ToolRegistry toolRegistry;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Environment env;

    @Value("${llm.provider}")
    private String provider;

    private String getConfig(String key) {
        return env.getProperty("llm." + provider + "." + key);
    }

    private String getModel(String purpose) {
        return env.getProperty("llm." + provider + ".models." + purpose);
    }

    /**
     * 对话接口（带历史记录）
     */
    public ParentChatResponse chat(ParentChatRequest request, ToolContext ctx,
            List<LlmRequest.Message> historyMessages) {
        List<LlmRequest.Message> messages = new ArrayList<>();

        // 1. 构建 System Prompt
        String childIdStr = ctx.childId() != null ? ctx.childId().toString() : "未知（请先确认孩子信息）";
        String historySummary = historyMessages == null || historyMessages.isEmpty()
            ? "暂无历史数据"
            : "有" + historyMessages.size() + "条对话历史";
        String systemPrompt = LlmPromptTemplate.parentChatSystemPromptV2(
            ctx.childName(), ctx.childAge(), ctx.childGrade(),
            "基础阶段", historySummary, childIdStr
        );
        messages.add(new LlmRequest.Message("system", systemPrompt));

        // 2. 添加历史消息
        if (historyMessages != null && !historyMessages.isEmpty()) {
            messages.addAll(historyMessages);
            log.info("LLM_HISTORY_ADDED | count={}", historyMessages.size());
        }

        // 3. 添加当前用户消息
        messages.add(new LlmRequest.Message("user", request.message()));

        return processChat(messages, ctx);
    }

    /**
     * 对话接口（无历史记录）
     */
    public ParentChatResponse chat(ParentChatRequest request, ToolContext ctx) {
        return chat(request, ctx, null);
    }

    private ParentChatResponse processChat(List<LlmRequest.Message> messages, ToolContext ctx) {
        int maxLoops = 5;
        for (int i = 0; i < maxLoops; i++) {
            log.info("LLM_LOOP | loop={} | messagesCount={}", i, messages.size());

            LlmRequest llmRequest = new LlmRequest(
                getModel("chat"),
                messages,
                4000,
                toolRegistry.all().stream()
                    .map(t -> new LlmRequest.ToolChoice(t.name(), t.description(), t.inputSchema()))
                    .toList()
            );

            LlmResponse response = callLlmApi(llmRequest);

            if (response == null) {
                log.error("LLM_API_ERROR | provider={} | response is null", provider);
                return new ParentChatResponse("抱歉，小星老师助手暂时无法提供服务，请稍后再试。", "text", null, null, null, false);
            }

            if (response.content() == null) {
                log.error("LLM_API_ERROR | provider={} | response.content is null | id={}", provider, response.id());
                return new ParentChatResponse("抱歉，小星老师助手暂时无法提供服务，请稍后再试。", "text", null, null, null, false);
            }

            if (response.usage() != null) {
                log.info("LLM_TOKEN_USAGE | provider={} | input={} | output={} | total={}",
                    provider, response.usage().inputTokens(), response.usage().outputTokens(), response.usage().totalTokens());
            }

            // 检查是否有 Tool Call
            LlmResponse.ContentBlock toolCall = response.content().stream()
                .filter(c -> "tool_use".equals(c.type()))
                .findFirst().orElse(null);

            if (toolCall != null) {
                log.info("LLM_TOOL_CALL | name={} | input={}", toolCall.name(), toolCall.input());
                JsonTool tool = toolRegistry.get(toolCall.name());
                if (tool != null) {
                    JsonNode toolResult = tool.execute(toolCall.input(), ctx);
                    log.info("LLM_TOOL_RESULT | name={} | result={}", tool.name(), toolResult);
                    messages.add(new LlmRequest.Message("assistant", "调用工具: " + tool.name()));
                    messages.add(new LlmRequest.Message("user", "工具返回结果: " + toolResult.toString()));
                    continue; // 继续循环
                } else {
                    log.warn("LLM_TOOL_NOT_FOUND | name={}", toolCall.name());
                }
            }

            // 如果没有 Tool Call，返回文本回复
            String reply = response.content().stream()
                .filter(c -> "text".equals(c.type()))
                .map(LlmResponse.ContentBlock::text)
                .findFirst().orElse("未获取到有效回复");

            log.info("LLM_REPLY | loop={} | reply='{}'", i, reply);
            return new ParentChatResponse(reply, "text", null, null, null, false);
        }

        log.warn("LLM_MAX_LOOPS_REACHED | loops={}", maxLoops);
        return new ParentChatResponse("对话轮次过多，请重新开始。", "text", null, null, null, false);
    }

    private LlmResponse callLlmApi(LlmRequest request) {
        String baseUrl = getConfig("base-url");
        String key = getConfig("api-key");

        if (baseUrl == null || key == null) {
            log.error("LLM_CONFIG_ERROR | provider={} | base-url={} | key={}", provider, baseUrl, key == null ? "null" : "set");
            return null;
        }

        // 默认认为是 OpenAI 格式 (Minimax, Kimi, OpenAI 都支持)
        boolean isAnthropicFormat = "anthropic".equals(provider);
        String url = baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions";

        if (isAnthropicFormat) {
            url = baseUrl.endsWith("/") ? baseUrl + "messages" : baseUrl + "/messages";
        }

        log.info("LLM_API_CALL | provider={} | model={} | messagesCount={} | url={}",
            provider, request.model(), request.messages().size(), url);

        try {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("model", request.model());
            body.put("max_tokens", request.maxTokens());

            if (!isAnthropicFormat) {
                headers.setBearerAuth(key);
                body.put("messages", request.messages());

                // MiniMax 特有：分离思考过程
                if ("minimax".equals(provider)) {
                    body.put("extra_body", Map.of("reasoning_split", true));
                }

                if (!request.tools().isEmpty()) {
                    body.put("tools", request.tools().stream().map(t -> Map.of(
                        "type", "function",
                        "function", Map.of(
                            "name", t.name(),
                            "description", t.description(),
                            "parameters", t.inputSchema()
                        )
                    )).toList());
                }
            } else {
                // Anthropic 格式 (Minimax 兼容模式也走这里)
                headers.set("x-api-key", key);
                headers.set("anthropic-version", "2023-06-01");

                // 提取 System Prompt
                String system = request.messages().stream()
                    .filter(m -> "system".equals(m.role()))
                    .map(LlmRequest.Message::content)
                    .findFirst().orElse("");

                List<Map<String, Object>> messages = request.messages().stream()
                    .filter(m -> !"system".equals(m.role()))
                    .map(m -> {
                        Map<String, Object> msg = new HashMap<>();
                        msg.put("role", m.role());
                        msg.put("content", m.content());
                        return msg;
                    })
                    .toList();

                if (!system.isEmpty()) body.put("system", system);
                body.put("messages", messages);

                if (!request.tools().isEmpty()) {
                    body.put("tools", request.tools().stream().map(t -> Map.of(
                        "name", t.name(),
                        "description", t.description(),
                        "input_schema", t.inputSchema()
                    )).toList());
                }
            }

            org.springframework.http.HttpEntity<Map<String, Object>> entity = new org.springframework.http.HttpEntity<>(body, headers);

            // 发送请求
            JsonNode rawResponse;
            try {
                rawResponse = restTemplate.postForObject(url, entity, JsonNode.class);
            } catch (Exception e) {
                log.error("LLM_HTTP_ERROR | url={} | error={} | class={}", url, e.getMessage(), e.getClass().getSimpleName());
                throw e;
            }

            if (rawResponse == null) {
                log.error("LLM_RESPONSE_NULL | url={}", url);
                return null;
            }

            log.info("LLM_RAW_RESPONSE_ID | id={} | hasChoices={}", rawResponse.get("id"), rawResponse.has("choices"));

            if (!isAnthropicFormat) {
                return parseOpenAiResponse(rawResponse);
            } else {
                return parseAnthropicResponse(rawResponse);
            }
        } catch (Exception e) {
            log.error("LLM_API_EXCEPTION | provider={} | error={} | class={}", provider, e.getMessage(), e.getClass().getSimpleName());
            throw new RuntimeException("LLM API 调用失败: " + e.getMessage(), e);
        }
    }

    private LlmResponse parseOpenAiResponse(JsonNode rawResponse) throws Exception {
        // 记录原始响应（关键调试日志）
        log.info("LLM_RAW_RESPONSE | {}", rawResponse);

        // 检查错误
        if (rawResponse.has("error")) {
            log.error("LLM_RESPONSE_ERROR | error={}", rawResponse.get("error"));
            throw new RuntimeException("LLM API 返回错误: " + rawResponse.get("error"));
        }

        if (!rawResponse.has("choices") || rawResponse.get("choices").isEmpty()) {
            log.error("LLM_RESPONSE_NO_CHOICES | raw={}", rawResponse);
            return null;
        }

        JsonNode choice = rawResponse.get("choices").get(0);
        if (choice == null || !choice.has("message")) {
            log.error("LLM_RESPONSE_INVALID_CHOICE | raw={}", rawResponse);
            return null;
        }

        JsonNode messageNode = choice.get("message");
        String content = messageNode.has("content") ? messageNode.get("content").asText() : "";

        // 过滤掉思考过程标签
        content = content.replaceAll("<think>[\\s\\S]*?</think>", "").trim();

        List<LlmResponse.ContentBlock> contentBlocks = new ArrayList<>();
        if (!content.isEmpty()) {
            contentBlocks.add(new LlmResponse.ContentBlock("text", content, null, null, null));
        }

        if (messageNode.has("tool_calls")) {
            JsonNode toolCalls = messageNode.get("tool_calls");
            log.info("LLM_TOOL_CALLS_COUNT | count={}", toolCalls.size());
            for (JsonNode tc : toolCalls) {
                JsonNode func = tc.get("function");
                contentBlocks.add(new LlmResponse.ContentBlock(
                    "tool_use", null,
                    func.get("name").asText(),
                    objectMapper.readTree(func.get("arguments").asText()),
                    tc.get("id").asText()
                ));
            }
        }

        JsonNode usageNode = rawResponse.get("usage");
        LlmResponse.Usage usage = null;
        if (usageNode != null && usageNode.has("prompt_tokens")) {
            usage = new LlmResponse.Usage(
                usageNode.get("prompt_tokens").asInt(),
                usageNode.get("completion_tokens").asInt(),
                usageNode.get("total_tokens").asInt()
            );
        }

        String finishReason = choice.has("finish_reason") ? choice.get("finish_reason").asText() : "unknown";
        log.info("LLM_PARSE_SUCCESS | finishReason={} | contentBlocks={}", finishReason, contentBlocks.size());

        return new LlmResponse(
            rawResponse.get("id").asText(),
            "message", "assistant",
            contentBlocks,
            rawResponse.get("model").asText(),
            finishReason,
            usage
        );
    }

    private LlmResponse parseAnthropicResponse(JsonNode rawResponse) {
        List<LlmResponse.ContentBlock> contentBlocks = new ArrayList<>();
        for (JsonNode block : rawResponse.get("content")) {
            String type = block.get("type").asText();
            if ("text".equals(type)) {
                contentBlocks.add(new LlmResponse.ContentBlock("text", block.get("text").asText(), null, null, null));
            } else if ("tool_use".equals(type)) {
                contentBlocks.add(new LlmResponse.ContentBlock(
                    "tool_use", null, 
                    block.get("name").asText(), 
                    block.get("input"),
                    block.get("id").asText()
                ));
            }
        }

        JsonNode usageNode = rawResponse.get("usage");
        LlmResponse.Usage usage = new LlmResponse.Usage(
            usageNode.get("input_tokens").asInt(),
            usageNode.get("output_tokens").asInt(),
            usageNode.get("input_tokens").asInt() + usageNode.get("output_tokens").asInt()
        );

        return new LlmResponse(
            rawResponse.get("id").asText(),
            "message", 
            rawResponse.get("role").asText(),
            contentBlocks,
            rawResponse.get("model").asText(),
            rawResponse.get("stop_reason").asText(),
            usage
        );
    }
}
