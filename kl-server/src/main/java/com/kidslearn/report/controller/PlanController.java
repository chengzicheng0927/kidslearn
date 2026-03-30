package com.kidslearn.report.controller;

import com.kidslearn.common.record.ApiResponse;
import com.kidslearn.report.dto.*;
import com.kidslearn.report.service.PlanService;
import com.kidslearn.user.repository.ChildRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import com.kidslearn.report.dto.PlanDetailDTO;

@RestController
@RequestMapping("/api/parent/plan")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;
    private final ChildRepository childRepository;

    @GetMapping("/plans")
    public ApiResponse<GetParentPlansResponse> getPlans(Authentication auth) {
        UUID parentId = (UUID) auth.getPrincipal();
        // 从 Token 获取家长关联的孩子（一爸一妈默认取第一个孩子）
        var child = childRepository.findByParentId(parentId).getFirst();
        UUID childId = child.getId();

        var plans = planService.getPlans(childId);
        var activePlan = plans.stream()
            .filter(p -> "active".equals(p.status()))
            .findFirst()
            .orElse(null);
        return ApiResponse.ok(new GetParentPlansResponse(plans, activePlan));
    }

    @GetMapping("/{planId}")
    public ApiResponse<PlanDetailDTO> getPlanDetail(@PathVariable UUID planId) {
        var detail = planService.getPlanDetail(planId);
        return ApiResponse.ok(detail);
    }

    @PostMapping("/confirm")
    public ApiResponse<LearningPlanDTO> confirmPlan(@RequestBody ConfirmPlanRequest req) {
        var plan = planService.confirmPlan(req.planId());
        return ApiResponse.ok(planService.toPlanDTO(plan));
    }

    @PostMapping("/pause")
    public ApiResponse<LearningPlanDTO> pausePlan(@RequestBody ConfirmPlanRequest req) {
        var plan = planService.pausePlan(req.planId());
        return ApiResponse.ok(planService.toPlanDTO(plan));
    }

    @PostMapping("/resume")
    public ApiResponse<LearningPlanDTO> resumePlan(@RequestBody ConfirmPlanRequest req) {
        var plan = planService.resumePlan(req.planId());
        return ApiResponse.ok(planService.toPlanDTO(plan));
    }

    @PostMapping("/cancel")
    public ApiResponse<LearningPlanDTO> cancelPlan(@RequestBody ConfirmPlanRequest req) {
        var plan = planService.cancelPlan(req.planId());
        return ApiResponse.ok(planService.toPlanDTO(plan));
    }

    @PostMapping("/{planId}/adjust-questions")
    public ApiResponse<Void> adjustQuestions(@PathVariable UUID planId,
                                             @RequestBody AdjustQuestionsRequest req) {
        planService.adjustQuestions(planId, req.removedQuestionIds());
        return ApiResponse.ok(null);
    }
}
