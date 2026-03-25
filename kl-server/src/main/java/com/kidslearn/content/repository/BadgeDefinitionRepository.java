package com.kidslearn.content.repository;

import com.kidslearn.content.domain.BadgeDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BadgeDefinitionRepository extends JpaRepository<BadgeDefinition, String> {
}
