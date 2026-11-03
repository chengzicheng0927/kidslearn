package com.kidslearn.content.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 题库预生成表（独立于 questions 表）。
 * questions 表用于实时 LLM 生成（灵活但慢）。
 * question_bank 用于预生成题目（固定 topic，快）。
 *
 * 激活计划时，从 question_bank 按 topic_key + difficulty + grade 精确匹配。
 */
@Entity
@Table(name = "question_bank")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class QuestionBank {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    /** 年级：一年级、二年级...（用于按孩子年级过滤） */
    @Column(nullable = false, length = 20)
    private String grade;

    @Column(nullable = false, length = 5)
    private String subject;

    /** 中文名：如 100以内加减法 */
    @Column(nullable = false, length = 100)
    private String topic;

    /** topic key：MA_ADD_SUB_100（来自 learning_stage_definitions.id） */
    @Column(name = "topic_key", nullable = false, length = 50)
    private String topicKey;

    @Column(nullable = false, length = 10)
    private String difficulty;

    /** 如 数学·100以内加减法·基础 */
    @Column(nullable = false)
    private String label;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<Map<String, String>> options;

    @Column(name = "correct_answer_id", nullable = false, length = 10)
    private String correctAnswerId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "explanation_steps", columnDefinition = "TEXT[]")
    private String[] explanationSteps;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String encouragement;

    @Column(name = "estimated_minutes", nullable = false)
    @Builder.Default
    private int estimatedMinutes = 3;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> reward = Map.of("stars", 8, "xp", 18);

    /** 状态：generating / approved / failed */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "approved";

    @Column(name = "generation_model", length = 50)
    private String generationModel;

    @Column(name = "generation_prompt", columnDefinition = "TEXT")
    private String generationPrompt;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (generatedAt == null) generatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
