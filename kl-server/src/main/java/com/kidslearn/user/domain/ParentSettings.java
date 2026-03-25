package com.kidslearn.user.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "parent_settings")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ParentSettings {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "parent_id", nullable = false, unique = true)
    private UUID parentId;

    @Column(name = "daily_limit_minutes", nullable = false)
    @Builder.Default
    private int dailyLimitMinutes = 30;

    @Column(name = "dnd_enabled", nullable = false)
    @Builder.Default
    private boolean dndEnabled = false;

    @Column(name = "dnd_start", nullable = false)
    @Builder.Default
    private LocalTime dndStart = LocalTime.of(21, 0);

    @Column(name = "dnd_end", nullable = false)
    @Builder.Default
    private LocalTime dndEnd = LocalTime.of(8, 0);

    @Column(name = "eye_care_reminder", nullable = false)
    @Builder.Default
    private boolean eyeCareReminder = true;

    @Column(name = "break_reminder", nullable = false)
    @Builder.Default
    private boolean breakReminder = true;

    @Column(name = "teacher_name", nullable = false, length = 50)
    @Builder.Default
    private String teacherName = "小星老师";

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        updatedAt = Instant.now();
    }
}
