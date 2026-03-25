package com.kidslearn.content.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "learning_stage_definitions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class LearningStageDefinition {

    @Id
    @Column(length = 50)
    private String id;

    @Column(nullable = false, length = 5)
    private String subject;

    @Column(nullable = false, length = 100)
    private String topic;

    @Column(name = "min_age", nullable = false)
    private int minAge;

    @Column(name = "max_age", nullable = false)
    private int maxAge;

    @Column(name = "next_stage_keys", columnDefinition = "TEXT[]")
    private String[] nextStageKeys;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
