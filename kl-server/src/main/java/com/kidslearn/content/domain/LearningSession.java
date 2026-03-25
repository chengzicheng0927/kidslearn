package com.kidslearn.content.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "learning_sessions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class LearningSession {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "child_id", nullable = false)
    private UUID childId;

    @Column(name = "plan_id")
    private UUID planId;

    @Column(name = "plan_phase_id")
    private UUID planPhaseId;

    @Column(name = "started_at", nullable = false)
    @Builder.Default
    private Instant startedAt = Instant.now();

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "total_questions", nullable = false)
    @Builder.Default
    private int totalQuestions = 0;

    @Column(name = "correct_count", nullable = false)
    @Builder.Default
    private int correctCount = 0;

    @Column(name = "xp_earned", nullable = false)
    @Builder.Default
    private int xpEarned = 0;

    @Column(name = "stars_earned", nullable = false)
    @Builder.Default
    private int starsEarned = 0;

    @Column(name = "current_combo", nullable = false)
    @Builder.Default
    private int currentCombo = 0;

    @Column(name = "longest_combo", nullable = false)
    @Builder.Default
    private int longestCombo = 0;
}
