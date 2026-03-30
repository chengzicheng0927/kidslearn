package com.kidslearn.content.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "child_wrong_questions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"child_id", "question_id"})
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class WrongQuestion {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "child_id", nullable = false)
    private UUID childId;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(name = "wrong_answer_id", nullable = false, length = 10)
    private String wrongAnswerId;

    @Column(name = "wrong_count", nullable = false)
    @Builder.Default
    private int wrongCount = 1;

    @Column(name = "correct_count", nullable = false)
    @Builder.Default
    private int correctCount = 0;

    @Column(name = "threshold", nullable = false)
    @Builder.Default
    private int threshold = 3;

    @Column(name = "answered_at")
    private Instant answeredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
