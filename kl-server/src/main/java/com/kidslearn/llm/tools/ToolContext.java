package com.kidslearn.llm.tools;

import java.util.UUID;

/**
 * 工具执行上下文
 */
public record ToolContext(
    UUID parentId,
    UUID childId,
    String childName,
    int childAge,
    String childAgeGroup,
    String subject
) {}
