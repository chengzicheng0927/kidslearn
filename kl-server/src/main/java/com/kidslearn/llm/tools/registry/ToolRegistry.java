package com.kidslearn.llm.tools.registry;

import com.kidslearn.llm.tools.JsonTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class ToolRegistry {

    private final Map<String, JsonTool> tools = new ConcurrentHashMap<>();

    public ToolRegistry(List<JsonTool> jsonTools) {
        jsonTools.forEach(this::register);
    }

    public void register(JsonTool tool) {
        tools.put(tool.name(), tool);
        log.info("TOOL_REGISTERED | name={}", tool.name());
    }

    public JsonTool get(String name) {
        return tools.get(name);
    }

    public List<JsonTool> all() {
        return List.copyOf(tools.values());
    }
}
