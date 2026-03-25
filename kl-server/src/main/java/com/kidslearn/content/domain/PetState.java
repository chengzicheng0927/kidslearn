package com.kidslearn.content.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "child_pets")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PetState {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "child_id", nullable = false, unique = true)
    private UUID childId;

    @Column(name = "pet_name", nullable = false, length = 50)
    @Builder.Default
    private String petName = "小星星";

    @Column(name = "evolution_id", nullable = false)
    @Builder.Default
    private int evolutionId = 1;

    @Column(nullable = false)
    @Builder.Default
    private int happiness = 80;

    @Column(nullable = false)
    @Builder.Default
    private int hunger = 50;

    @Column(nullable = false)
    @Builder.Default
    private int xp = 0;

    @Column(name = "xp_to_next", nullable = false)
    @Builder.Default
    private int xpToNext = 100;

    @Column(name = "last_fed_at")
    private Instant lastFedAt;

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
