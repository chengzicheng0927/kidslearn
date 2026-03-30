package com.kidslearn.llm.tools;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kidslearn.report.domain.LearningPlan;
import com.kidslearn.report.service.PlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PlanGenerateTool implements JsonTool {

    private final PlanService planService;
    private final ObjectMapper objectMapper;

    @Override
    public String name() {
        return "generate_study_plan";
    }

    @Override
    public String description() {
        return "根据家长要求生成结构化的学习计划。包含多个阶段，每个阶段有主题、目标、每天题目数和难度。";
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
        phaseProps.putObject("topic").put("type", "string").put("description", "阶段主题");
        phaseProps.putObject("goal").put("type", "string").put("description", "阶段目标");
        phaseProps.putObject("questionsPerDay").put("type", "integer").put("description", "每天练习题目数");
        phaseProps.putObject("difficulty").put("type", "string").set("enum",
                objectMapper.createArrayNode().add("easy").add("medium").add("hard"));
        phaseItems.putArray("required").add("dayRange").add("topic").add("goal").add("questionsPerDay")
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

            List<Map<String, Object>> phases = objectMapper.convertValue(
                    input.get("phases"), new TypeReference<List<Map<String, Object>>>() {
                    });

            log.info("TOOL_EXECUTE | tool=generate_study_plan | childId={} | subject={} | duration={}",
                    childId, subject, durationDays);

            int perDay = phases.isEmpty() ? 10 : (int) phases.get(0).getOrDefault("questionsPerDay", 10);

            LearningPlan plan = LearningPlan.builder()
                    .subject(subject)
                    .durationDays(durationDays)
                    .difficulty(difficulty)
                    .phases(phases)
                    .dailyGoal(String.valueOf(perDay))
                    .estimatedMinutes(durationDays * 15) // 粗略估算
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
}
