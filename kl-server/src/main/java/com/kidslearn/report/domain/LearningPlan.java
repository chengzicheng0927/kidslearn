package com.kidslearn.report.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "learning_plans")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class LearningPlan {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "child_id", nullable = false)
    private UUID childId;

    @Column(nullable = false, length = 5)
    private String subject;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "pending";

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String difficulty = "medium";

    @Column(name = "duration_days", nullable = false)
    private int durationDays;

    @Column(name = "daily_goal", nullable = false)
    private String dailyGoal;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private List<Map<String, Object>> phases = List.of();

    @Column(name = "current_phase_index", nullable = false)
    @Builder.Default
    private int currentPhaseIndex = 0;

    @Column(name = "estimated_minutes", nullable = false)
    @Builder.Default
    private int estimatedMinutes = 20;

    @Column(name = "generation_prompt", columnDefinition = "TEXT")
    private String generationPrompt;

    @Column(name = "generation_model", length = 50)
    private String generationModel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "generation_params", columnDefinition = "jsonb")
    private Map<String, Object> generationParams;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "paused_at")
    private Instant pausedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "total_completed", nullable = false)
    @Builder.Default
    private int totalCompleted = 0;

    @Column(name = "total_correct", nullable = false)
    @Builder.Default
    private int totalCorrect = 0;

    @Column(name = "current_streak", nullable = false)
    @Builder.Default
    private int currentStreak = 0;

    @Column(name = "longest_streak", nullable = false)
    @Builder.Default
    private int longestStreak = 0;

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
