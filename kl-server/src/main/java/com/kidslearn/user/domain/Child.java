package com.kidslearn.user.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "children")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Child {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "parent_id", nullable = false)
    private UUID parentId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private int age;

    @Column(name = "age_group", nullable = false, length = 10)
    private String ageGroup;

    @Column(nullable = false)
    @Builder.Default
    private int stars = 0;

    @Column(nullable = false)
    @Builder.Default
    private int streak = 0;

    @Column(name = "daily_target_en", nullable = false)
    @Builder.Default
    private int dailyTargetEn = 5;

    @Column(name = "daily_target_ma", nullable = false)
    @Builder.Default
    private int dailyTargetMa = 3;

    @Column(name = "today_minutes", nullable = false)
    @Builder.Default
    private int todayMinutes = 0;

    @Column(name = "today_date", nullable = false)
    @Builder.Default
    private LocalDate todayDate = LocalDate.now();

    @Column(nullable = false)
    @Builder.Default
    private int xp = 0;

    @Column(name = "xp_needed", nullable = false)
    @Builder.Default
    private int xpNeeded = 100;

    @Column(nullable = false)
    @Builder.Default
    private int level = 1;

    @Column(name = "total_answered", nullable = false)
    @Builder.Default
    private int totalAnswered = 0;

    @Column(name = "total_days", nullable = false)
    @Builder.Default
    private int totalDays = 0;

    @Column(name = "reminder_enabled", nullable = false)
    @Builder.Default
    private boolean reminderEnabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(length = 20)
    private String grade;

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

    /**
     * 根据年龄计算年龄段
     */
    public static String calcAgeGroup(int age) {
        if (age <= 4) return "3-4岁";
        if (age <= 6) return "5-6岁";
        if (age <= 8) return "7-8岁";
        if (age <= 10) return "9-10岁";
        if (age <= 12) return "11-12岁";
        return "13-14岁";
    }
}
