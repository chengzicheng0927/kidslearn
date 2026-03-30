package com.kidslearn.llm.tools;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kidslearn.content.domain.PlanQuestion;
import com.kidslearn.content.domain.Question;
import com.kidslearn.content.repository.PlanQuestionRepository;
import com.kidslearn.content.repository.QuestionRepository;
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
    private final ObjectMapper objectMapper;

    @Override
    public String name() {
        return "generate_questions";
    }

    @Override
    public String description() {
        return "为学习计划的特定阶段生成配套题目。请提供包含题目内容、选项、正确答案和解析的完整列表。";
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
        qProps.putObject("difficulty").put("type", "string");
        qProps.putObject("label").put("type", "string");
        qProps.putObject("question").put("type", "string");
        qProps.putObject("correctAnswerId").put("type", "string");
        qProps.putObject("explanation").put("type", "string");
        qProps.putObject("encouragement").put("type", "string");

        ObjectNode optionItems = objectMapper.createObjectNode();
        optionItems.put("type", "object");
        ObjectNode optProps = optionItems.putObject("properties");
        optProps.putObject("id").put("type", "string");
        optProps.putObject("text").put("type", "string");

        qProps.putObject("options").put("type", "array").set("items", optionItems);
        questionItems.putArray("required").add("topic").add("difficulty").add("label").add("question").add("options")
                .add("correctAnswerId").add("explanation").add("encouragement");

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

            JsonNode questionsNode = input.get("questions");
            List<UUID> savedQuestionIds = new ArrayList<>();

            log.info("TOOL_EXECUTE | tool=generate_questions | planId={} | phaseIndex={} | count={}",
                    planId, phaseIndex, questionsNode.size());

            for (JsonNode qNode : questionsNode) {
                Question question = Question.builder()
                        .subject(subject)
                        .topic(qNode.get("topic").asText())
                        .topicKey(qNode.get("topic").asText()) // 简单处理
                        .difficulty(qNode.get("difficulty").asText())
                        .label(qNode.get("label").asText())
                        .question(qNode.get("question").asText())
                        .options(objectMapper.convertValue(qNode.get("options"),
                                new TypeReference<List<Map<String, String>>>() {
                                }))
                        .correctAnswerId(qNode.get("correctAnswerId").asText())
                        .explanation(qNode.get("explanation").asText())
                        .encouragement(qNode.get("encouragement").asText())
                        .estimatedMinutes(3)
                        .reward(Map.of("stars", 8, "xp", 18))
                        .auditStatus("pending")
                        .build();

                question = questionRepository.save(question);
                if (question.getId() == null)
                    continue; // Should not happen
                savedQuestionIds.add(question.getId());

                PlanQuestion pq = PlanQuestion.builder()
                        .planId(planId)
                        .questionId(question.getId())
                        .phaseIndex(phaseIndex)
                        .purpose("learning")
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
