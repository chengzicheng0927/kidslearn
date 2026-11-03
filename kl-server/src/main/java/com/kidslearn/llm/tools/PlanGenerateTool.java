package com.kidslearn.llm.tools;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kidslearn.content.repository.LearningStageRepository;
import com.kidslearn.report.domain.LearningPlan;
import com.kidslearn.report.service.PlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PlanGenerateTool implements JsonTool {

    private final PlanService planService;
    private final LearningStageRepository stageRepository;
    private final ObjectMapper objectMapper;

    @Override
    public String name() {
        return "generate_study_plan";
    }

    @Override
    public String description() {
        return "根据家长要求创建学习计划，返回 planId 用于后续生成题目。计划包含多个阶段，每个阶段有 topicKey（必须从可用列表中选择）、topic（中文名）、goal、questionsPerDay、difficulty。家长确认计划后，再用 planId 调用 generate_questions 生成各阶段题目。topicKey 必须从系统提供的可选列表中选取，禁止自行编造！";
    }

    @Override
    public JsonNode inputSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");

        properties.putObject("subject")
                .put("type", "string")
                .set("enum", objectMapper.createArrayNode().add("en").add("ma"));
        ((ObjectNode) properties.get("subject"))
                .put("description", "学科：en(英语) 或 ma(数学)");

        properties.putObject("childId")
                .put("type", "string")
                .put("format", "uuid");

        properties.putObject("durationDays")
                .put("type", "integer")
                .put("minimum", 7)
                .put("maximum", 30)
                .put("description", "计划持续天数");

        properties.putObject("difficulty")
                .put("type", "string")
                .set("enum", objectMapper.createArrayNode().add("easy").add("medium").add("hard"));

        ObjectNode phaseItems = objectMapper.createObjectNode();
        phaseItems.put("type", "object");
        ObjectNode phaseProps = phaseItems.putObject("properties");
        phaseProps.putObject("dayRange").put("type", "string").put("description", "天数范围，如 '1-7'");
        phaseProps.putObject("topic").put("type", "string").put("description", "阶段主题中文名，如 100以内加减法");
        phaseProps.putObject("topicKey").put("type", "string").put("description", "topic key，必须从可选列表中选择（如 MA_ADD_SUB_100），禁止自行编造");
        phaseProps.putObject("goal").put("type", "string").put("description", "阶段目标");
        phaseProps.putObject("questionsPerDay").put("type", "integer").put("description", "每天练习题目数");
        phaseProps.putObject("difficulty").put("type", "string").set("enum",
                objectMapper.createArrayNode().add("easy").add("medium").add("hard"));
        phaseItems.putArray("required").add("dayRange").add("topic").add("topicKey").add("goal").add("questionsPerDay")
                .add("difficulty");

        properties.putObject("phases")
                .put("type", "array")
                .set("items", phaseItems);

        schema.putArray("required").add("subject").add("childId").add("durationDays").add("phases").add("difficulty");
        return schema;
    }

    @Override
    public JsonNode execute(JsonNode input, ToolContext ctx) {
        try {
            UUID childId = UUID.fromString(input.get("childId").asText());
            String subject = input.get("subject").asText();
            int durationDays = input.get("durationDays").asInt();
            String difficulty = input.get("difficulty").asText();

            JsonNode phasesNode = input.get("phases");
            if (phasesNode.isTextual()) {
                log.info("TOOL_PARSE_RETRY | tool=generate_study_plan | phases field is string, parsing...");
                phasesNode = objectMapper.readTree(phasesNode.asText());
            }

            List<Map<String, Object>> phases = objectMapper.convertValue(
                    phasesNode, new TypeReference<List<Map<String, Object>>>() {
                    });

            // 校验 topicKey 是否合法
            Set<String> validTopicKeys = Set.copyOf(
                stageRepository.findBySubject(subject).stream()
                    .map(s -> s.getId())
                    .toList()
            );

            for (int i = 0; i < phases.size(); i++) {
                Map<String, Object> phase = phases.get(i);
                String topicKey = String.valueOf(phase.getOrDefault("topicKey", ""));
                if (!validTopicKeys.contains(topicKey)) {
                    log.warn("TOOL_INVALID_TOPIC_KEY | phaseIndex={} | topicKey={} | valid={}",
                        i, topicKey, validTopicKeys);
                    // 如果 topicKey 无效，从 topic 名称模糊匹配
                    String topic = String.valueOf(phase.getOrDefault("topic", ""));
                    String matched = matchTopicByName(subject, topic, validTopicKeys);
                    if (matched != null) {
                        phase.put("topicKey", matched);
                        log.info("TOOL_TOPIC_KEY_FIXED | phaseIndex={} | topic={} | matched={}", i, topic, matched);
                    }
                }
            }

            log.info("TOOL_EXECUTE | tool=generate_study_plan | childId={} | subject={} | duration={}",
                    childId, subject, durationDays);

            int perDay = phases.isEmpty() ? 10 : (int) phases.get(0).getOrDefault("questionsPerDay", 10);

            LearningPlan plan = LearningPlan.builder()
                    .subject(subject)
                    .durationDays(durationDays)
                    .difficulty(difficulty)
                    .phases(phases)
                    .dailyGoal(String.valueOf(perDay))
                    .estimatedMinutes(perDay * 3) // 每日时长 = 每日题目数 × 每题约3分钟
                    .build();

            LearningPlan savedPlan = planService.createPendingPlan(ctx.parentId(), childId, plan);

            ObjectNode result = objectMapper.createObjectNode();
            result.put("success", true);
            result.put("planId", savedPlan.getId().toString());
            result.put("status", savedPlan.getStatus());
            result.put("summary", "学习计划已生成，等待您的确认。");
            return result;
        } catch (Exception e) {
            log.error("TOOL_ERROR | tool=generate_study_plan | error={}", e.getMessage());
            ObjectNode result = objectMapper.createObjectNode();
            result.put("success", false);
            result.put("message", e.getMessage());
            return result;
        }
    }

    /**
     * 根据 topic 名称模糊匹配合法的 topicKey。
     * 如果 LLM 没有正确传递 topicKey，尝试从 topic 名称匹配。
     */
    private String matchTopicByName(String subject, String topicName, Set<String> validTopicKeys) {
        if (topicName == null || topicName.isBlank()) return null;
        var stages = stageRepository.findBySubject(subject);
        for (var s : stages) {
            if (s.getTopic().contains(topicName) || topicName.contains(s.getTopic())) {
                return s.getId();
            }
        }
        // 模糊匹配关键字
        for (var s : stages) {
            String[] keywords = {"乘法", "除法", "加减", "分数", "小数", "应用", "动物", "水果", "食物",
                "颜色", "数字", "对话", "阅读", "语法", "词汇"};
            for (String kw : keywords) {
                if (s.getTopic().contains(kw) && topicName.contains(kw)) {
                    return s.getId();
                }
            }
        }
        return null;
    }
}
