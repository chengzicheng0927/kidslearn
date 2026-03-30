package com.kidslearn.content.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "questions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Question {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, length = 5)
    private String subject;

    @Column(nullable = false, length = 100)
    private String topic;

    @Column(name = "topic_key", nullable = false, length = 50)
    private String topicKey;

    @Column(nullable = false, length = 10)
    private String difficulty;

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
    private Map<String, Object> reward = Map.of();

    @Column(name = "success_result_mode", length = 20)
    private String successResultMode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "stage_bridge", columnDefinition = "jsonb")
    private Map<String, Object> stageBridge;

    @Column(name = "generation_model", length = 50)
    private String generationModel;

    @Column(name = "generation_prompt", columnDefinition = "TEXT")
    private String generationPrompt;

    @Column(name = "llm_response_id", length = 100)
    private String llmResponseId;

    @Column(name = "audit_status", nullable = false, length = 20)
    @Builder.Default
    private String auditStatus = "pending";

    @Column(name = "audit_note", columnDefinition = "TEXT")
    private String auditNote;

    @Column(name = "audited_at")
    private Instant auditedAt;

    @Column(name = "audited_by")
    private UUID auditedBy;

    @Column(name = "times_shown", nullable = false)
    @Builder.Default
    private int timesShown = 0;

    @Column(name = "times_correct", nullable = false)
    @Builder.Default
    private int timesCorrect = 0;

    @Column(insertable = false, updatable = false)
    private BigDecimal accuracy;

    @Column(columnDefinition = "TEXT[]")
    private String[] tags;

    /**
     * 题目计划日期。题目在创建学习计划时预分配到具体日期，
     * 取题时只取 scheduledDate = today 的题目。
     * 为 null 表示未分配日期的题目（老数据或临时题）。
     */
    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
