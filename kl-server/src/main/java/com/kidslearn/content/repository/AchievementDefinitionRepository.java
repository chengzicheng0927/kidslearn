package com.kidslearn.content.repository;

import com.kidslearn.content.domain.AchievementDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AchievementDefinitionRepository extends JpaRepository<AchievementDefinition, String> {
}
