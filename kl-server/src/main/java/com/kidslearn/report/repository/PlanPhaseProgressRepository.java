package com.kidslearn.report.repository;

import com.kidslearn.report.domain.PlanPhaseProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PlanPhaseProgressRepository extends JpaRepository<PlanPhaseProgress, UUID> {
    List<PlanPhaseProgress> findByPlanIdOrderByPhaseIndex(UUID planId);
}
