package com.kidslearn.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * 宠物喂食事件
 */
public record PetFedEvent(
    UUID childId,
    String foodItemId,
    Instant fedAt
) {}
