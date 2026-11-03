package com.kidslearn.llm.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kidslearn.content.service.QuestionBankService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 题库预生成 Tool。
 * 用于管理员手动触发生成，或在特定对话场景下触发。
 *
 * 注意：这是异步工具，调用后立即返回 taskId，不等待生成完成。
 * 实际生成由 QuestionBankService 异步执行。
 */
@Component
@Slf4j
public class GenerateBankQuestionsTool implements JsonTool {

    private final QuestionBankService questionBankService;
    private final ObjectMapper objectMapper;

    @Autowired
    public GenerateBankQuestionsTool(ObjectMapper objectMapper, @Lazy QuestionBankService questionBankService) {
        this.objectMapper = objectMapper;
        this.questionBankService = questionBankService;
    }

    @Override
    public String name() {
        return "generate_bank_questions";
    }

    @Override
    public String description() {
        return "为题库预生成题目。根据 topicKey 生成指定数量的选择题，保存到题库。题库是共享的，不会为任何孩子绑定题目。生成是异步的，调用后立即返回任务ID，实际生成在后台完成。需要传入 topic_key（如 MA_ADD_SUB_100，来自 learning_stage_definitions）、difficulty（easy/medium/hard）和生成数量。";
    }

    @Override
    public JsonNode inputSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");

        properties.putObject("topicKey")
                .put("type", "string")
                .put("description", "topic key，如 MA_ADD_SUB_100（来自 learning_stage_definitions）");

        properties.putObject("difficulty")
                .put("type", "string")
                .set("enum", objectMapper.createArrayNode().add("easy").add("medium").add("hard"));

        properties.putObject("count")
                .put("type", "integer")
                .put("minimum", 1)
                .put("maximum", 100)
                .put("description", "生成题目数量，默认50");

        properties.putObject("grade")
                .put("type", "string")
                .put("description", "年级，如 一年级、二年级，用于限制题库生成的年级范围");

        schema.putArray("required").add("topicKey").add("difficulty");
        return schema;
    }

    @Override
    public JsonNode execute(JsonNode input, ToolContext ctx) {
        try {
            String topicKey = input.get("topicKey").asText();
            String difficulty = input.get("difficulty").asText();
            int count = input.has("count") ? input.get("count").asInt() : 50;

            log.info("TOOL_EXECUTE | tool=generate_bank_questions | topicKey={} | difficulty={} | count={}",
                topicKey, difficulty, count);

            UUID taskId = questionBankService.triggerGeneration(topicKey, difficulty, count);

            ObjectNode result = objectMapper.createObjectNode();
            if (taskId != null) {
                result.put("success", true);
                result.put("taskId", taskId.toString());
                result.put("status", "generating");
                result.put("message", "题库生成任务已创建，将在后台异步生成");
            } else {
                result.put("success", true);
                result.put("status", "already_generating");
                result.put("message", "该 topic+difficulty 已有进行中的生成任务，跳过重复创建");
            }
            return result;
        } catch (Exception e) {
            log.error("TOOL_ERROR | tool=generate_bank_questions | error={}", e.getMessage());
            ObjectNode result = objectMapper.createObjectNode();
            result.put("success", false);
            result.put("message", e.getMessage());
            return result;
        }
    }
}
