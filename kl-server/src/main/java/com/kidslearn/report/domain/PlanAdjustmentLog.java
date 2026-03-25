package com.kidslearn.report.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "plan_adjustment_log")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PlanAdjustmentLog {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(name = "adjustment_prompt", nullable = false, columnDefinition = "TEXT")
    private String adjustmentPrompt;

    @Column(name = "llm_response", columnDefinition = "TEXT")
    private String llmResponse;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "phases_before", columnDefinition = "jsonb")
    private Object phasesBefore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "phases_after", columnDefinition = "jsonb")
    private Object phasesAfter;

    @Column(name = "adjustment_type", length = 20)
    private String adjustmentType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
