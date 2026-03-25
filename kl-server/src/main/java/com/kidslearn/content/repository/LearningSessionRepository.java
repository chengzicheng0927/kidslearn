package com.kidslearn.content.repository;

import com.kidslearn.content.domain.LearningSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LearningSessionRepository extends JpaRepository<LearningSession, UUID> {

    Optional<LearningSession> findTopByChildIdAndEndedAtIsNullOrderByStartedAtDesc(UUID childId);
}
