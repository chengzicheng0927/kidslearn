package com.kidslearn.report.controller;

import com.kidslearn.common.record.ApiResponse;
import com.kidslearn.content.dto.QuestionBankDTO;
import com.kidslearn.content.repository.LearningStageRepository;
import com.kidslearn.content.service.QuestionBankService;
import com.kidslearn.report.domain.LearningPlan;
import com.kidslearn.report.dto.*;
import com.kidslearn.report.repository.LearningPlanRepository;
import com.kidslearn.report.service.PlanService;
import com.kidslearn.user.repository.ChildRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.kidslearn.report.dto.PlanDetailDTO;

@RestController
@RequestMapping("/api/parent/plan")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;
    private final ChildRepository childRepository;
    private final LearningPlanRepository planRepository;
    private final ObjectProvider<QuestionBankService> questionBankServiceProvider;
    private final LearningStageRepository stageRepository;

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

    /**
     * 从题库匹配并分配题目
     * POST /api/parent/plan/{planId}/generate-questions
     */
    @PostMapping("/{planId}/generate-questions")
    public ApiResponse<GenerateQuestionsFromBankResponse> generateQuestionsFromBank(
            @PathVariable UUID planId) {

        // 获取计划详情
        LearningPlan plan = planRepository.findById(planId)
            .orElseThrow(() -> new com.kidslearn.common.exception.ResourceNotFoundException("LearningPlan", planId));

        // 获取孩子年级
        var child = childRepository.findById(plan.getChildId()).orElse(null);
        String grade = child != null && child.getGrade() != null ? child.getGrade() : "二年级";

        // 从题库分配题目
        var bankService = questionBankServiceProvider.getObject();
        var assignResult = bankService.assignQuestionsFromBank(
            planId, plan.getChildId(), grade, plan.getSubject(), plan.getPhases()
        );

        // 获取分配后的计划详情
        var planDetail = planService.getPlanDetail(planId);

        // 计算已分配的题目总数
        int totalAssigned = planDetail.phases().stream()
            .mapToInt(p -> p.questions().size())
            .sum();

        // topic key 转成中文名
        Map<String, String> topicNameMap = stageRepository.findAll().stream()
            .collect(Collectors.toMap(
                com.kidslearn.content.domain.LearningStageDefinition::getId,
                com.kidslearn.content.domain.LearningStageDefinition::getTopic,
                (a, b) -> a
            ));
        List<String> insufficientTopicNames = assignResult.insufficientTopicKeys().stream()
            .map(k -> topicNameMap.getOrDefault(k, k))
            .toList();

        // 构建响应
        GenerateQuestionsFromBankResponse response = new GenerateQuestionsFromBankResponse(
            planDetail,
            assignResult.assignedCount(),
            assignResult.success(),
            insufficientTopicNames,
            assignResult.message(),
            totalAssigned
        );

        return ApiResponse.ok(response);
    }

    /** 从题库生成题目的响应 */
    public record GenerateQuestionsFromBankResponse(
        PlanDetailDTO plan,             // 计划详情（含题目）
        int assignedCount,              // 本次分配的题目数
        boolean success,               // 是否成功
        List<String> insufficientTopicKeys, // 题目不足的 topic
        String message,                // 结果描述
        int totalQuestions             // 计划总题目数
    ) {}
}
