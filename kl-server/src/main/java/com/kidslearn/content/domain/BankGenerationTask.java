package com.kidslearn.content.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * 题库生成任务表。
 * 跟踪异步生成任务状态：pending → generating → completed/failed。
 *
 * 通过 unique index (topic_key + difficulty) WHERE status IN ('pending', 'generating')
 * 确保同一 topic+difficulty 不会并发生成。
 */
@Entity
@Table(name = "bank_generation_task")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class BankGenerationTask {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    /** 年级 */
    @Column(length = 20)
    private String grade;

    /** 科目 */
    @Column(length = 5)
    private String subject;

    /** 主题 key（来自 learning_stage_definitions.id） */
    @Column(name = "topic_key", nullable = false, length = 50)
    private String topicKey;

    /** 难度：easy / medium / hard */
    @Column(nullable = false, length = 10)
    private String difficulty;

    /** 目标数量（补充到 min-waterline） */
    @Column(name = "target_count", nullable = false)
    @Builder.Default
    private int targetCount = 50;

    /** 已生成数量 */
    @Column(name = "questions_generated", nullable = false)
    @Builder.Default
    private int questionsGenerated = 0;

    /** 状态：pending / generating / completed / failed */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "pending";

    /** 失败时的错误信息 */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
