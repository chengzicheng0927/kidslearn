package com.kidslearn.report.repository;

import com.kidslearn.report.domain.LearningPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LearningPlanRepository extends JpaRepository<LearningPlan, UUID> {

    List<LearningPlan> findByChildIdOrderByCreatedAtDesc(UUID childId);

    List<LearningPlan> findByChildIdAndStatus(UUID childId, String status);
}
