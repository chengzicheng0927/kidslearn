package com.kidslearn.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * 成就解锁事件
 */
public record AchievementEarnedEvent(
    UUID childId,
    String achievementId,
    Instant earnedAt
) {}
