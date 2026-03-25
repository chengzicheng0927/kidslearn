package com.kidslearn.report.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "study_records")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class StudyRecord {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "child_id", nullable = false)
    private UUID childId;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(nullable = false, length = 5)
    private String subject;

    @Column(nullable = false, length = 100)
    private String topic;

    @Column(name = "question_count", nullable = false)
    @Builder.Default
    private int questionCount = 0;

    @Column(name = "correct_count", nullable = false)
    @Builder.Default
    private int correctCount = 0;

    @Column(insertable = false, updatable = false)
    private BigDecimal accuracy;

    @Column(name = "stars_earned", nullable = false)
    @Builder.Default
    private int starsEarned = 0;

    @Column(nullable = false)
    @Builder.Default
    private int minutes = 0;

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
