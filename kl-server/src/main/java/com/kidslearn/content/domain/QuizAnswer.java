package com.kidslearn.content.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "quiz_answers")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class QuizAnswer {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(name = "plan_question_id")
    private UUID planQuestionId;

    @Column(name = "selected_answer", nullable = false, length = 10)
    private String selectedAnswer;

    @Column(name = "is_correct", nullable = false)
    private boolean isCorrect;

    @Column(name = "time_spent_sec", nullable = false)
    @Builder.Default
    private int timeSpentSec = 0;

    @Column(name = "snapshot_topic", length = 100)
    private String snapshotTopic;

    @Column(name = "snapshot_stage_key", length = 50)
    private String snapshotStageKey;

    @Column(name = "snapshot_purpose", length = 20)
    private String snapshotPurpose;

    @Column(name = "combo_at_answer", nullable = false)
    @Builder.Default
    private int comboAtAnswer = 0;

    @Column(name = "answered_at", nullable = false)
    @Builder.Default
    private Instant answeredAt = Instant.now();
}
