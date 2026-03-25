package com.kidslearn.report.repository;

import com.kidslearn.report.domain.PlanAdjustmentLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PlanAdjustmentLogRepository extends JpaRepository<PlanAdjustmentLog, UUID> {
    List<PlanAdjustmentLog> findByPlanIdOrderByCreatedAtDesc(UUID planId);
}
