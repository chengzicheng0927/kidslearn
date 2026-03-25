package com.kidslearn.content.dto;

import java.util.List;

/**
 * 知识星球徽章响应
 */
public record BadgeDTO(
    List<BadgeItem> badges,
    List<String> earnedIds,
    int totalAnswered,
    int currentLevel
) {
    public record BadgeItem(
        String id,
        int level,
        String icon,
        String title,
        String description,
        int requirement
    ) {}
}
