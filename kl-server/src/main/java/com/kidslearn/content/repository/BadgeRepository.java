package com.kidslearn.content.repository;

import com.kidslearn.content.domain.Badge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BadgeRepository extends JpaRepository<Badge, UUID> {

    List<Badge> findByChildId(UUID childId);

    boolean existsByChildIdAndBadgeId(UUID childId, String badgeId);
}
