package com.kidslearn.content.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "child_badges")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Badge {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "child_id", nullable = false)
    private UUID childId;

    @Column(name = "badge_id", nullable = false, length = 50)
    private String badgeId;

    @Column(name = "earned_at", nullable = false)
    private Instant earnedAt;

    @PrePersist
    protected void onCreate() {
        if (earnedAt == null) earnedAt = Instant.now();
    }
}
