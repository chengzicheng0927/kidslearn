package com.kidslearn.content.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "child_achievements")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Achievement {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "child_id", nullable = false)
    private UUID childId;

    @Column(name = "definition_id", nullable = false, length = 50)
    private String definitionId;

    @Column(name = "icon", length = 10)
    private String icon;

    @Column(name = "title", length = 100)
    private String title;

    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "progress_pct", nullable = false)
    @Builder.Default
    private int progressPct = 0;

    @Column(name = "earned_at")
    private Instant earnedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
