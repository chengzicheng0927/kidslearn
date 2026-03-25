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

@RestController
@RequestMapping("/api/parent/plan")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;
    private final ChildRepository childRepository;

    @GetMapping("s")
    public ApiResponse<List<LearningPlanDTO>> getPlans(@RequestParam UUID childId) {
        return ApiResponse.ok(planService.getPlans(childId));
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
}
