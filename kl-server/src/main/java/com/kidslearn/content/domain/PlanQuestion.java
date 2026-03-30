package com.kidslearn.content.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "plan_questions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PlanQuestion {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String purpose = "practice";

    @Column(name = "phase_index", nullable = false)
    @Builder.Default
    private int phaseIndex = 0;

    @Column(name = "wrong_question_id")
    private UUID wrongQuestionId;

    @Column(name = "added_at", nullable = false, updatable = false)
    private Instant addedAt;

    @PrePersist
    protected void onCreate() {
        if (addedAt == null) addedAt = Instant.now();
    }
}
