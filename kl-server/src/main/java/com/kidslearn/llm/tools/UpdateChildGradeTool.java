package com.kidslearn.llm.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kidslearn.user.service.ChildService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class UpdateChildGradeTool implements JsonTool {

    private final ChildService childService;
    private final ObjectMapper objectMapper;

    @Override
    public String name() {
        return "update_child_grade";
    }

    @Override
    public String description() {
        return "更新孩子的年级信息到数据库。当家长明确告知孩子年级时调用此工具。";
    }

    @Override
    public JsonNode inputSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        
        properties.putObject("childId")
            .put("type", "string")
            .put("format", "uuid")
            .put("description", "孩子的唯一标识 ID");
            
        properties.putObject("grade")
            .put("type", "string")
            .put("description", "孩子的年级，例如：'一年级'、'2年级'、'Grade 3'");
            
        schema.putArray("required").add("childId").add("grade");
        return schema;
    }

    @Override
    public JsonNode execute(JsonNode input, ToolContext ctx) {
        try {
            UUID childId = UUID.fromString(input.get("childId").asText());
            String grade = input.get("grade").asText();
            
            log.info("TOOL_EXECUTE | tool=update_child_grade | childId={} | grade={}", childId, grade);
            childService.updateGrade(childId, grade);
            
            ObjectNode result = objectMapper.createObjectNode();
            result.put("success", true);
            result.put("childId", childId.toString());
            result.put("grade", grade);
            return result;
        } catch (Exception e) {
            log.error("TOOL_ERROR | tool=update_child_grade | error={}", e.getMessage());
            ObjectNode result = objectMapper.createObjectNode();
            result.put("success", false);
            result.put("message", e.getMessage());
            return result;
        }
    }
}
