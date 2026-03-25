package com.kidslearn.content.repository;

import com.kidslearn.content.domain.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AchievementRepository extends JpaRepository<Achievement, UUID> {

    List<Achievement> findByChildId(UUID childId);

    boolean existsByChildIdAndDefinitionId(UUID childId, String definitionId);
}
