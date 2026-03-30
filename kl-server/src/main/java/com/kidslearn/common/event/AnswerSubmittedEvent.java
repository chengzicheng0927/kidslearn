package com.kidslearn.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * 答题提交事件
 * 触发时机：QuizService.doSubmit() 中答题结果写入后
 * 订阅者：AchievementService、PetService、PlanPhaseProgressService
 */
public record AnswerSubmittedEvent(
    UUID childId,
    UUID sessionId,
    UUID questionId,
    UUID planQuestionId,
    String selectedAnswer,
    boolean isCorrect,
    int comboAtAnswer,
    String subject,
    String topicKey,
    Instant answeredAt
) {}
