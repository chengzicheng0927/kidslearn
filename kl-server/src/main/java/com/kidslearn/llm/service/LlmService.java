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
import com.kidslearn.report.dto.PlanDetailDTO;
import com.kidslearn.report.service.PlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LlmService {

    private final ToolRegistry toolRegistry;
    private final PlanService planService;
    private final com.kidslearn.content.repository.LearningStageRepository stageRepository;
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
     * 对话接口（带历史记录 + 外部传入 conversationId）
     */
    public ParentChatResponse chat(ParentChatRequest request, ToolContext ctx,
            List<LlmRequest.Message> historyMessages, String conversationId) {
        List<LlmRequest.Message> messages = new ArrayList<>();

        if (conversationId == null || conversationId.isBlank()) {
            throw new IllegalStateException("conversationId must be provided by controller");
        }

        // 1. 构建 System Prompt（含动态 topic 列表）
        String childIdStr = ctx.childId() != null ? ctx.childId().toString() : "未知（请先确认孩子信息）";
        String historySummary = historyMessages == null || historyMessages.isEmpty()
            ? "暂无历史数据"
            : "有" + historyMessages.size() + "条对话历史";
        String subject = ctx.subject();
        String topicList = buildTopicListForPrompt(subject, ctx.childAge());
        int topicCount = topicList.trim().isEmpty() ? 0 : topicList.split("\n").length;
        log.info("TOPIC_LIST_INJECTED | subject={} | age={} | topicCount={}", subject != null ? subject : "ALL", ctx.childAge(), topicCount);
        log.debug("TOPIC_LIST_CONTENT:\n{}", topicList);

        // topic 列表为空时，直接返回提示，不调 LLM
        if (topicCount == 0) {
            log.info("TOPIC_LIST_EMPTY | age={} | returning direct response", ctx.childAge());
            return new com.kidslearn.chat.dto.ParentChatResponse(
                "您好！目前系统暂未配置适合 " + ctx.childAge() + "岁 孩子的学习计划，我们会尽快补充，感谢您的支持！",
                "text", null, null, null, false, conversationId
            );
        }

        String systemPrompt = LlmPromptTemplate.parentChatSystemPromptV3(
            ctx.childName(), ctx.childAge(), ctx.childGrade(),
            "基础阶段", historySummary, childIdStr, topicList
        );
        messages.add(new LlmRequest.Message("system", systemPrompt));

        // 2. 添加历史消息
        if (historyMessages != null && !historyMessages.isEmpty()) {
            messages.addAll(historyMessages);
            log.info("LLM_HISTORY_ADDED | count={}", historyMessages.size());
        }

        // 3. 添加当前用户消息
        messages.add(new LlmRequest.Message("user", request.message()));

        return processChat(messages, ctx, conversationId);
    }

    /**
     * 对话接口（带历史记录，从 request 读取 conversationId）
     */
    public ParentChatResponse chat(ParentChatRequest request, ToolContext ctx,
            List<LlmRequest.Message> historyMessages) {
        return chat(request, ctx, historyMessages, request.conversationId());
    }

    /**
     * 对话接口（无历史记录）
     */
    public ParentChatResponse chat(ParentChatRequest request, ToolContext ctx) {
        return chat(request, ctx, null);
    }

    private ParentChatResponse processChat(List<LlmRequest.Message> messages, ToolContext ctx, String conversationId) {
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
                return new ParentChatResponse("抱歉，小星老师助手暂时无法提供服务，请稍后再试。", "text", null, null, null, false, conversationId);
            }

            if (response.content() == null) {
                log.error("LLM_API_ERROR | provider={} | response.content is null | id={}", provider, response.id());
                return new ParentChatResponse("抱歉，小星老师助手暂时无法提供服务，请稍后再试。", "text", null, null, null, false, conversationId);
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

                    // generate_study_plan 成功后，直接构建卡片返回，不再让 LLM 生成文本
                    if ("generate_study_plan".equals(tool.name())
                            && toolResult.has("success") && toolResult.get("success").asBoolean()
                            && toolResult.has("planId")) {

                        // 从数据库查询已保存的计划，确保数据准确
                        UUID planId = UUID.fromString(toolResult.get("planId").asText());
                        var plan = planService.findById(planId);
                        String subject = plan.getSubject();
                        String difficulty = plan.getDifficulty();
                        int durationDays = plan.getDurationDays();
                        int estimatedMinutes = plan.getEstimatedMinutes();
                        List<Map<String, Object>> phases = plan.getPhases();

                        // 构建 plan（传给前端）
                        Map<String, Object> planData = new HashMap<>();
                        planData.put("id", planId);
                        planData.put("subject", subject);
                        planData.put("duration", durationDays);
                        planData.put("difficulty", difficulty);
                        planData.put("estimatedMinutes", estimatedMinutes);
                        planData.put("phases", phases);

                        // 构建 rows
                        List<ParentChatResponse.ChatCardDTO.RowDTO> rows = new ArrayList<>();
                        rows.add(new ParentChatResponse.ChatCardDTO.RowDTO(
                                "科目", "en".equals(subject) ? "英语" : "数学"));
                        rows.add(new ParentChatResponse.ChatCardDTO.RowDTO(
                                "周期", durationDays + "天"));
                        rows.add(new ParentChatResponse.ChatCardDTO.RowDTO(
                                "难度", "easy".equals(difficulty) ? "基础" : "medium".equals(difficulty) ? "中等" : "困难"));
                        rows.add(new ParentChatResponse.ChatCardDTO.RowDTO(
                                "每日时长", "约" + estimatedMinutes + "分钟"));

                        // 构建 phases
                        List<ParentChatResponse.ChatCardDTO.PhaseDetailDTO> cardPhases = new ArrayList<>();
                        for (Map<String, Object> p : phases) {
                            cardPhases.add(new ParentChatResponse.ChatCardDTO.PhaseDetailDTO(
                                    String.valueOf(p.get("dayRange")),
                                    String.valueOf(p.get("topic")),
                                    (Integer) p.get("questionsPerDay"),
                                    String.valueOf(p.get("difficulty")),
                                    null
                            ));
                        }

                        // 构建 actions - 还没有题目，展示"生成题目"按钮
                        List<ParentChatResponse.ChatCardDTO.ActionDTO> actions = new ArrayList<>();
                        actions.add(new ParentChatResponse.ChatCardDTO.ActionDTO("生成题目", "confirm_generate_questions"));

                        // 构建 card（标题带日期）
                        String subjectLabel = "en".equals(subject) ? "英语" : "数学";
                        String dateStr = DateTimeFormatter.ofPattern("MM-dd")
                                .withZone(ZoneId.systemDefault())
                                .format(plan.getCreatedAt());
                        String cardTitle = dateStr + " " + subjectLabel + "学习计划";

                        ParentChatResponse.ChatCardDTO cardDTO = new ParentChatResponse.ChatCardDTO(
                                "plan",
                                cardTitle,
                                "📋",
                                rows,
                                cardPhases,
                                actions
                        );

                        String reply = toolResult.has("summary")
                                ? toolResult.get("summary").asText()
                                : "学习计划已生成，请确认。";

                        log.info("LLM_PLAN_CARD_BUILT | planId={}", planId);
                        return new ParentChatResponse(reply, "card", cardDTO, planData, null, false, conversationId);
                    }

                    // generate_questions 成功后，直接返回带"确认计划"按钮的卡片，不再让 LLM 继续循环
                    if ("generate_questions".equals(tool.name())
                            && toolResult.has("success") && toolResult.get("success").asBoolean()) {

                        UUID planId = UUID.fromString(toolResult.get("planId").asText());
                        int count = toolResult.has("count") ? toolResult.get("count").asInt() : 0;

                        // 调用 PlanService 获取计划详情（含题目）
                        PlanDetailDTO planDetail = planService.getPlanDetail(planId);

                        // 将 PlanDetailDTO.PlanPhaseDetailDTO 映射为 ChatCardDTO.PhaseDetailDTO
                        List<ParentChatResponse.ChatCardDTO.PhaseDetailDTO> cardPhases = planDetail.phases().stream()
                                .map(p -> new ParentChatResponse.ChatCardDTO.PhaseDetailDTO(
                                        p.dayRange(),
                                        p.topic(),
                                        p.questionsPerDay(),
                                        p.difficulty(),
                                        p.questions().stream()
                                                .map(q -> new ParentChatResponse.ChatCardDTO.QuestionDTO(
                                                        q.id().toString(),
                                                        q.icon(),
                                                        q.question(),
                                                        q.options(),
                                                        q.answer() != null ? q.answer() : ""))
                                                .toList()
                                ))
                                .toList();

                        // 构建 actions - 已有题目，展示"确认计划"按钮
                        List<ParentChatResponse.ChatCardDTO.ActionDTO> actions = new ArrayList<>();
                        actions.add(new ParentChatResponse.ChatCardDTO.ActionDTO("确认计划", "confirm_plan"));

                        // 构建 card（带题目数据，展开卡片时可查看详情）
                        ParentChatResponse.ChatCardDTO cardDTO = new ParentChatResponse.ChatCardDTO(
                                "plan",
                                "题目生成完成",
                                "✅",
                                List.of(new ParentChatResponse.ChatCardDTO.RowDTO("已生成", count + "道练习题")),
                                cardPhases,
                                actions
                        );

                        String reply = count > 0
                                ? "题目已生成完成，共 " + count + " 道练习题，可以开始学习啦！"
                                : "题目生成完成。";

                        Map<String, Object> planData = new HashMap<>();
                        planData.put("id", planId.toString());
                        planData.put("subject", planDetail.subject());
                        planData.put("duration", planDetail.durationDays());
                        planData.put("phases", planDetail.phases().stream()
                                .map(p -> Map.<String, Object>of(
                                        "dayRange", p.dayRange(),
                                        "topic", p.topic(),
                                        "questionsPerDay", p.questionsPerDay(),
                                        "difficulty", p.difficulty()))
                                .toList());

                        return new ParentChatResponse(reply, "card", cardDTO, planData, "confirm", false, conversationId);
                    }

                    // 其他 tool 继续循环
                    messages.add(new LlmRequest.Message("assistant", "调用工具: " + tool.name()));
                    messages.add(new LlmRequest.Message("user", "工具返回结果: " + toolResult.toString()));
                    continue;
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
            return new ParentChatResponse(reply, "text", null, null, null, false, conversationId);
        }

        log.warn("LLM_MAX_LOOPS_REACHED | loops={}", maxLoops);
        return new ParentChatResponse("对话轮次过多，请重新开始。", "text", null, null, null, false, conversationId);
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

    /**
     * 根据科目和年龄构建 topic 列表，注入到 system prompt 中。
     * - subject 为 null 时：显示该年龄适用的全部 topic（英语+数学）
     * - subject 有值时：只显示该科目的 topic
     * LLM 生成计划时必须从以下 topicKey 中选择，禁止自己编造 topicKey。
     */
    private String buildTopicListForPrompt(String subject, int age) {
        var baseList = subject != null
            ? stageRepository.findBySubject(subject)
            : stageRepository.findAll();
        var relevant = baseList.stream()
            .filter(s -> s.getMinAge() <= age && s.getMaxAge() >= age)
            .toList();

        if (relevant.isEmpty()) {
            relevant = baseList;
        }

        StringBuilder sb = new StringBuilder();
        for (var s : relevant) {
            String subjectLabel = "ma".equals(s.getSubject()) ? "数学" : "英语";
            sb.append(String.format("  - %s: %s（%s，年龄 %d-%d岁）\n",
                s.getId(), s.getTopic(), subjectLabel, s.getMinAge(), s.getMaxAge()));
        }
        return sb.toString();
    }

    /**
     * 为题库预生成题目（无工具调用，纯文本返回）。
     * 用于 BankGenerationRunner 异步生成题库。
     *
     * @param topicKey    topic key（如 MA_ADD_SUB_100）
     * @param topicName   中文名（如 100以内加减法）
     * @param subject     科目（en / ma）
     * @param difficulty  难度（easy / medium / hard）
     * @param count       生成数量
     * @return 生成的题目列表，每项含 question、options、correctAnswerId 等字段
     */
    public List<Map<String, Object>> generateBankQuestions(
            String topicKey, String topicName, String subject, String difficulty, int count) {

        String systemPrompt = bankGenerationPrompt(topicName, subject, difficulty);
        String userPrompt = String.format(
            "请为\"%s\"（topic_key: %s）生成 %d 道 %s 难度的选择题，返回标准 JSON 数组格式。",
            topicName, topicKey, count, difficulty
        );

        List<LlmRequest.Message> messages = List.of(
            new LlmRequest.Message("system", systemPrompt),
            new LlmRequest.Message("user", userPrompt)
        );

        // 每道题约 300 tokens（题干+选项+解析+步骤+鼓励语），加 buffer
        int maxTokens = Math.min(count * 400 + 500, 8000);
        LlmRequest request = new LlmRequest(getModel("chat"), messages, maxTokens, List.of());

        // 重试机制：LLM 服务过载（529）时最多重试 3 次，指数退避
        LlmResponse response = null;
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                response = callLlmApi(request);
                break; // 成功则跳出
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : "";
                boolean retryable = msg.contains("529") || msg.contains("overloaded") || msg.contains("rate_limit") || msg.contains("503");
                if (retryable && attempt < maxRetries) {
                    long waitMs = attempt * 5000L; // 5s, 10s, 15s
                    log.warn("BANK_LLM_RETRY | topicKey={} | attempt={}/{} | waitMs={} | error={}",
                        topicKey, attempt, maxRetries, waitMs, msg);
                    try { Thread.sleep(waitMs); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                } else {
                    throw e;
                }
            }
        }

        if (response == null || response.content() == null || response.content().isEmpty()) {
            log.error("BANK_LLM_ERROR | topicKey={} | response={}", topicKey, response);
            throw new RuntimeException("题库生成 LLM 调用失败");
        }

        // 提取文本内容
        String textContent = response.content().stream()
            .filter(c -> "text".equals(c.type()))
            .map(LlmResponse.ContentBlock::text)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("LLM 未返回文本内容"));

        // 剥离 markdown 代码块包裹（LLM 常返回 ```json ... ```）
        String jsonText = stripMarkdownCodeBlock(textContent);

        // 解析 JSON 数组
        try {
            var node = objectMapper.readTree(jsonText);
            return objectMapper.convertValue(node, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.error("BANK_PARSE_ERROR | topicKey={} | error={} | text={}", topicKey, e.getMessage(), jsonText.substring(0, Math.min(200, jsonText.length())));
            throw new RuntimeException("题库生成 JSON 解析失败: " + e.getMessage());
        }
    }

    /**
     * 剥离 LLM 返回文本中的 markdown 代码块包裹。
     * 处理 ```json\n...\n``` 和 ```\n...\n``` 两种格式。
     */
    private String stripMarkdownCodeBlock(String text) {
        String trimmed = text.strip();
        if (trimmed.startsWith("```")) {
            // 去掉开头的 ```json 或 ```
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            // 去掉结尾的 ```
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
            return trimmed.strip();
        }
        return trimmed;
    }

    private String bankGenerationPrompt(String topicName, String subject, String difficulty) {
        String subjectDesc = "en".equals(subject) ? "英语" : "数学";
        String prompt = """
            你是一个专业的小学%s题目生成器。

            【生成要求】
            - 严格围绕"%s"这个主题出题
            - 难度级别：%s
            - 必须生成选择题，每道题 4 个选项（A/B/C/D）
            - 正确答案随机分布，不固定位置
            - 题干清晰，适合儿童理解
            - 乘法除法题尽量结合生活情境

            【返回格式】标准 JSON 数组，每项结构如下：
            [
              {
                "label": "%s·%s·%s",
                "question": "题目内容（不含选项）",
                "options": [
                  {"id": "a", "text": "选项A内容"},
                  {"id": "b", "text": "选项B内容"},
                  {"id": "c", "text": "选项C内容"},
                  {"id": "d", "text": "选项D内容"}
                ],
                "correctAnswerId": "a",
                "explanation": "解析内容",
                "explanationSteps": ["步骤1", "步骤2"],
                "encouragement": "鼓励语"
              }
            ]

            【禁止】
            - 不要在题干中出现选项内容
            - 不要生成非 JSON 的其他文字说明
            """;
        return String.format(prompt, subjectDesc, topicName, difficulty, subjectDesc, topicName, difficulty);
    }
}
