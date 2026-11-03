package com.kidslearn.content.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 题库管理 DTO
 */
public class QuestionBankDTO {

    /** 单个 topic 的统计 */
    public record TopicStats(
        String topicKey,
        String topic,
        String subject,
        long total,
        long approved,
        long generating,
        long failed,
        Instant lastGeneratedAt
    ) {}

    /** 全部统计响应 */
    public record BankStatsResponse(
        List<TopicStats> topics
    ) {}

    /** 触发生成响应 */
    public record GenerateResponse(
        UUID taskId,
        String status,
        String message
    ) {}

    /** 单个任务的详情 */
    public record TaskInfo(
        UUID taskId,
        String topicKey,
        String difficulty,
        int targetCount,
        int questionsGenerated,
        String status,
        String errorMessage,
        Instant createdAt,
        Instant completedAt
    ) {}

    /** 从题库分配题目的结果 */
    public record AssignResult(
        int assignedCount,              // 成功分配的题目数量
        int insufficientTopics,         // 题目不足的 topic 数
        List<String> insufficientTopicKeys, // 哪些 topic 不足
        boolean success,               // 是否成功（有题目被分配）
        String message                  // 结果描述
    ) {}
}
