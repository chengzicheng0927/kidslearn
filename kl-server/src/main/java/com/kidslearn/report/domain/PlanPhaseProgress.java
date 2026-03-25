package com.kidslearn.report.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "plan_phase_progress")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PlanPhaseProgress {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(name = "phase_index", nullable = false)
    private int phaseIndex;

    @Column(name = "phase_key", nullable = false, length = 50)
    private String phaseKey;

    @Column(name = "phase_topic", nullable = false, length = 100)
    private String phaseTopic;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "questions_planned", nullable = false)
    @Builder.Default
    private int questionsPlanned = 0;

    @Column(name = "questions_done", nullable = false)
    @Builder.Default
    private int questionsDone = 0;

    @Column(name = "correct_count", nullable = false)
    @Builder.Default
    private int correctCount = 0;

    @Column(insertable = false, updatable = false)
    private BigDecimal accuracy;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String difficulty = "medium";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> extra;

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
