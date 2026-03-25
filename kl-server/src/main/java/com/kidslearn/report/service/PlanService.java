package com.kidslearn.report.service;

import com.kidslearn.common.exception.BusinessException;
import com.kidslearn.common.exception.ResourceNotFoundException;
import com.kidslearn.report.domain.LearningPlan;
import com.kidslearn.report.dto.LearningPlanDTO;
import com.kidslearn.report.repository.LearningPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlanService {

    private final LearningPlanRepository planRepository;

    public List<LearningPlanDTO> getPlans(UUID childId) {
        return planRepository.findByChildIdOrderByCreatedAtDesc(childId)
            .stream().map(this::toPlanDTO).toList();
    }

    public LearningPlan findById(UUID planId) {
        return planRepository.findById(planId)
            .orElseThrow(() -> new ResourceNotFoundException("LearningPlan", planId));
    }

    @Transactional
    public LearningPlan createPendingPlan(UUID parentId, UUID childId, LearningPlan plan) {
        plan.setChildId(childId);
        plan.setStatus("pending");
        return planRepository.save(plan);
    }

    @Transactional
    public LearningPlan confirmPlan(UUID planId) {
        LearningPlan plan = findById(planId);
        if (!"pending".equals(plan.getStatus())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_STATUS", "计划不在待确认状态");
        }
        plan.setStatus("active");
        plan.setStartedAt(Instant.now());
        return planRepository.save(plan);
    }

    @Transactional
    public LearningPlan pausePlan(UUID planId) {
        LearningPlan plan = findById(planId);
        plan.setStatus("paused");
        plan.setPausedAt(Instant.now());
        return planRepository.save(plan);
    }

    @Transactional
    public LearningPlan resumePlan(UUID planId) {
        LearningPlan plan = findById(planId);
        plan.setStatus("active");
        plan.setPausedAt(null);
        return planRepository.save(plan);
    }

    @Transactional
    public LearningPlan cancelPlan(UUID planId) {
        LearningPlan plan = findById(planId);
        plan.setStatus("cancelled");
        plan.setCancelledAt(Instant.now());
        return planRepository.save(plan);
    }

    @SuppressWarnings("unchecked")
    public LearningPlanDTO toPlanDTO(LearningPlan p) {
        List<LearningPlanDTO.PlanPhaseDTO> phases = p.getPhases().stream()
            .map(phase -> new LearningPlanDTO.PlanPhaseDTO(
                (String) phase.getOrDefault("dayRange", ""),
                (String) phase.getOrDefault("goal", ""),
                (String) phase.getOrDefault("topic", ""),
                phase.containsKey("questionsPerDay") ? ((Number) phase.get("questionsPerDay")).intValue() : 5,
                (String) phase.getOrDefault("difficulty", "medium")
            ))
            .toList();

        return new LearningPlanDTO(
            p.getId(), p.getSubject(), p.getDurationDays(), p.getDailyGoal(),
            phases, p.getDifficulty(), p.getEstimatedMinutes(), p.getStatus(),
            toLocalDate(p.getCreatedAt()), toLocalDate(p.getStartedAt()), toLocalDate(p.getCompletedAt())
        );
    }

    private LocalDate toLocalDate(Instant instant) {
        return instant != null ? instant.atZone(ZoneId.systemDefault()).toLocalDate() : null;
    }
}
