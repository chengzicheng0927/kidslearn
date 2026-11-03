package com.kidslearn.llm.tools;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kidslearn.content.domain.PlanQuestion;
import com.kidslearn.content.domain.Question;
import com.kidslearn.content.repository.PlanQuestionRepository;
import com.kidslearn.content.repository.QuestionRepository;
import com.kidslearn.report.repository.LearningPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class QuestionGenerateTool implements JsonTool {

    private final QuestionRepository questionRepository;
    private final PlanQuestionRepository planQuestionRepository;
    private final LearningPlanRepository learningPlanRepository;
    private final ObjectMapper objectMapper;

    @Override
    public String name() {
        return "generate_questions";
    }

    @Override
    public String description() {
        return "【重要前置条件】必须先调用 generate_study_plan 创建学习计划并获得 planId 后，才能调用本工具。planId 必须来自 generate_study_plan 的返回值，禁止自己编造 planId。【核心职责】本工具仅负责将题目保存到数据库，不会生成题目。LLM 必须先根据计划阶段主题自己生成题目，再通过 questions 参数传入本工具保存。每道题必须包含 topicKey（从可选列表中选择，如 MA_ADD_SUB_100）。本工具生成完成后直接告知家长题目已生成即可，不要询问完成进度、不要讨论学习计划进展、不要引导后续操作。";
    }

    @Override
    public JsonNode inputSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");

        properties.putObject("planId").put("type", "string").put("format", "uuid");
        properties.putObject("phaseIndex").put("type", "integer");
        properties.putObject("subject").put("type", "string").set("enum",
                objectMapper.createArrayNode().add("en").add("ma"));

        ObjectNode questionItems = objectMapper.createObjectNode();
        questionItems.put("type", "object");
        ObjectNode qProps = questionItems.putObject("properties");
        qProps.putObject("topic").put("type", "string");
        qProps.putObject("topicKey").put("type", "string").put("description", "topic key，如 MA_ADD_SUB_100（必须从可选列表中选择）");
        qProps.putObject("difficulty").put("type", "string");
        qProps.putObject("label").put("type", "string");
        qProps.putObject("question").put("type", "string");
        qProps.putObject("correctAnswerId").put("type", "string");
        qProps.putObject("explanation").put("type", "string");
        qProps.putObject("explanationSteps")
                .put("type", "array")
                .set("items", objectMapper.createObjectNode().put("type", "string"));
        qProps.putObject("encouragement").put("type", "string");

        ObjectNode optionItems = objectMapper.createObjectNode();
        optionItems.put("type", "object");
        ObjectNode optProps = optionItems.putObject("properties");
        optProps.putObject("id").put("type", "string");
        optProps.putObject("text").put("type", "string");

        qProps.putObject("options").put("type", "array").set("items", optionItems);
        questionItems.putArray("required").add("topic").add("difficulty").add("label").add("question").add("options")
                .add("correctAnswerId").add("explanation").add("explanationSteps").add("encouragement");

        properties.putObject("questions")
                .put("type", "array")
                .set("items", questionItems);

        schema.putArray("required").add("planId").add("phaseIndex").add("subject").add("questions");
        return schema;
    }

    @Override
    @Transactional
    public JsonNode execute(JsonNode input, ToolContext ctx) {
        try {
            UUID planId = UUID.fromString(input.get("planId").asText());
            int phaseIndex = input.get("phaseIndex").asInt();
            String subject = input.get("subject").asText();

            // 校验 plan 是否存在
            if (!learningPlanRepository.existsById(planId)) {
                log.warn("TOOL_ERROR | tool=generate_questions | plan not found | planId={}", planId);
                ObjectNode result = objectMapper.createObjectNode();
                result.put("success", false);
                result.put("message", "学习计划不存在，请检查 planId 是否正确。planId=" + planId);
                return result;
            }

            JsonNode questionsNode = input.get("questions");
            if (questionsNode == null) {
                log.error("TOOL_ERROR | tool=generate_questions | questions field is null in input");
                return objectMapper.createObjectNode().put("success", false).put("message", "questions field is missing");
            }

            if (questionsNode.isTextual()) {
                log.info("TOOL_PARSE_RETRY | tool=generate_questions | questions field is string, parsing...");
                questionsNode = objectMapper.readTree(questionsNode.asText());
            }

            List<UUID> savedQuestionIds = new ArrayList<>();

            log.info("TOOL_EXECUTE | tool=generate_questions | planId={} | phaseIndex={} | count={}",
                    planId, phaseIndex, questionsNode.size());

            for (JsonNode qNode : questionsNode) {
                // 优先使用 topicKey，否则 fallback 到 topic 字符串
                String topic = qNode.has("topic") ? qNode.get("topic").asText() : "";
                String topicKey = qNode.has("topicKey") ? qNode.get("topicKey").asText() : topic;

                Question question = Question.builder()
                        .subject(subject)
                        .topic(topic)
                        .topicKey(topicKey)
                        .difficulty(qNode.get("difficulty").asText())
                        .label(qNode.get("label").asText())
                        .question(qNode.get("question").asText())
                        .options(objectMapper.convertValue(qNode.get("options"),
                                new TypeReference<List<Map<String, String>>>() {
                                }))
                        .correctAnswerId(qNode.get("correctAnswerId").asText())
                        .explanation(qNode.get("explanation").asText())
                        .explanationSteps(objectMapper.convertValue(qNode.get("explanationSteps"), String[].class))
                        .encouragement(qNode.get("encouragement").asText())
                        .estimatedMinutes(3)
                        .reward(Map.of("stars", 8, "xp", 18))
                        .auditStatus("pending")
                        .tags(new String[]{topicKey})
                        .build();

                question = questionRepository.save(question);
                if (question.getId() == null)
                    continue; // Should not happen
                savedQuestionIds.add(question.getId());

                PlanQuestion pq = PlanQuestion.builder()
                        .planId(planId)
                        .questionId(question.getId())
                        .phaseIndex(phaseIndex)
                        .purpose("practice")
                        .build();
                planQuestionRepository.save(pq);
            }

            ObjectNode result = objectMapper.createObjectNode();
            result.put("success", true);
            result.put("planId", planId.toString());
            result.put("phaseIndex", phaseIndex);
            result.put("count", savedQuestionIds.size());
            return result;
        } catch (Exception e) {
            log.error("TOOL_ERROR | tool=generate_questions | error={}", e.getMessage());
            ObjectNode result = objectMapper.createObjectNode();
            result.put("success", false);
            result.put("message", e.getMessage());
            return result;
        }
    }
}
