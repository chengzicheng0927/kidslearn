package com.kidslearn.content.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;

@Entity
@Table(name = "badge_definitions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class BadgeDefinition {

    @Id
    @Column(length = 50)
    private String id;

    @Column(nullable = false, unique = true)
    private Integer level;

    @Column(length = 20)
    private String icon;

    @Column(length = 100)
    private String title;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private Integer requirement;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
