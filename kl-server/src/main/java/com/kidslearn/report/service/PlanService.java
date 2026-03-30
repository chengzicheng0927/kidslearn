package com.kidslearn.report.service;

import com.kidslearn.common.exception.BusinessException;
import com.kidslearn.common.exception.ResourceNotFoundException;
import com.kidslearn.content.domain.PlanQuestion;
import com.kidslearn.content.domain.Question;
import com.kidslearn.content.repository.PlanQuestionRepository;
import com.kidslearn.content.repository.QuestionRepository;
import com.kidslearn.report.domain.LearningPlan;
import com.kidslearn.report.dto.LearningPlanDTO;
import com.kidslearn.report.dto.PlanDetailDTO;
import com.kidslearn.report.repository.LearningPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlanService {

    private final LearningPlanRepository planRepository;
    private final QuestionRepository questionRepository;
    private final PlanQuestionRepository planQuestionRepository;

    public List<LearningPlanDTO> getPlans(UUID childId) {
        return planRepository.findByChildIdOrderByCreatedAtDesc(childId)
            .stream().map(this::toPlanDTO).toList();
    }

    public LearningPlan findById(UUID planId) {
        return planRepository.findById(planId)
            .orElseThrow(() -> new ResourceNotFoundException("LearningPlan", planId));
    }

    /**
     * 获取计划详情（含题目列表）
     */
    @Transactional(readOnly = true)
    public PlanDetailDTO getPlanDetail(UUID planId) {
        LearningPlan plan = findById(planId);

        // 计算正确率
        double accuracy = plan.getTotalCompleted() > 0
            ? Math.round(plan.getTotalCorrect() * 100.0 / plan.getTotalCompleted() * 10) / 10.0
            : 0.0;

        // 构建阶段详情
        List<PlanDetailDTO.PlanPhaseDetailDTO> phaseDetails = new ArrayList<>();
        List<Map<String, Object>> phases = plan.getPhases();
        boolean isCompleted = "completed".equals(plan.getStatus());

        for (int i = 0; i < phases.size(); i++) {
            Map<String, Object> phase = phases.get(i);
            int phaseIndex = i;

            // 获取该阶段的题目
            List<PlanQuestion> planQuestions = planQuestionRepository.findByPlanId(planId).stream()
                .filter(pq -> pq.getPhaseIndex() == phaseIndex)
                .toList();

            List<PlanDetailDTO.QuestionDetailDTO> questionDetails = new ArrayList<>();
            for (PlanQuestion pq : planQuestions) {
                Question q = questionRepository.findById(pq.getQuestionId()).orElse(null);
                if (q != null) {
                    // 构建选项列表
                    List<String> options = q.getOptions().stream()
                        .map(opt -> opt.getOrDefault("text", ""))
                        .toList();

                    // 已完成的计划返回正确答案
                    String answer = isCompleted ? getOptionTextById(q, q.getCorrectAnswerId()) : null;

                    questionDetails.add(new PlanDetailDTO.QuestionDetailDTO(
                        q.getId(),
                        getQuestionIcon(q.getTopic()),
                        q.getQuestion(),
                        options,
                        answer
                    ));
                }
            }

            phaseDetails.add(new PlanDetailDTO.PlanPhaseDetailDTO(
                phaseIndex,
                (String) phase.getOrDefault("dayRange", ""),
                (String) phase.getOrDefault("goal", ""),
                (String) phase.getOrDefault("topic", ""),
                phase.containsKey("questionsPerDay") ? ((Number) phase.get("questionsPerDay")).intValue() : 5,
                (String) phase.getOrDefault("difficulty", "medium"),
                questionDetails
            ));
        }

        return new PlanDetailDTO(
            plan.getId(),
            plan.getSubject(),
            plan.getStatus(),
            plan.getDifficulty(),
            plan.getDurationDays(),
            plan.getDailyGoal(),
            phaseDetails,
            plan.getEstimatedMinutes(),
            toLocalDate(plan.getStartedAt()),
            toLocalDate(plan.getCompletedAt()),
            plan.getTotalCompleted(),
            plan.getTotalCorrect(),
            accuracy
        );
    }

    /**
     * 根据题目和选项 ID 获取选项文本
     */
    private String getOptionTextById(Question q, String optionId) {
        return q.getOptions().stream()
            .filter(opt -> optionId.equals(opt.get("id")))
            .findFirst()
            .map(opt -> opt.getOrDefault("text", "").toString())
            .orElse("");
    }

    /**
     * 根据话题获取图标
     */
    private String getQuestionIcon(String topic) {
        if (topic == null) return "📝";
        return switch (topic.toLowerCase()) {
            case "动物", "animals" -> "🐱";
            case "水果", "fruits" -> "🍎";
            case "颜色", "colors" -> "🔴";
            case "数字", "numbers" -> "1️⃣";
            case "食物", "food" -> "🍕";
            case "家庭", "family" -> "👨‍👩‍👧";
            default -> "📝";
        };
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

        // 将题库中的题打散分配到计划周期的每一天
        assignQuestionsToPlan(plan);

        return planRepository.save(plan);
    }

    /**
     * 将题库中的题按计划周期打散分配到每天。
     * 从 plan 开始日期起，均匀分配到 durationDays 天中。
     */
    private void assignQuestionsToPlan(LearningPlan plan) {
        LocalDate startDate = Instant.now().atZone(ZoneId.systemDefault()).toLocalDate();
        int duration = plan.getDurationDays();

        // 从题库取已审核通过的题目（按 subject）
        List<Question> pool = questionRepository.findBySubjectAndDifficulty(
            plan.getSubject(), plan.getDifficulty()
        );
        // 过滤已有 scheduledDate 的题目（已分配过）
        pool = pool.stream()
            .filter(q -> q.getScheduledDate() == null)
            .toList();

        if (pool.isEmpty()) {
            log.warn("PLAN_ASSIGN_NO_QUESTIONS | planId={} | subject={}", plan.getId(), plan.getSubject());
            return;
        }

        // 打乱顺序
        List<Question> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled);

        int totalQuestions = shuffled.size();
        int questionsPerDay = Math.max(1, totalQuestions / duration);

        for (int i = 0; i < duration && i < shuffled.size(); i++) {
            // 每天分配 questionsPerDay 道题（最后一天分完剩余的）
            int dayEnd = Math.min((i + 1) * questionsPerDay, shuffled.size());
            for (int j = i * questionsPerDay; j < dayEnd; j++) {
                Question q = shuffled.get(j);
                q.setScheduledDate(startDate.plusDays(i));
            }
        }

        questionRepository.saveAll(shuffled.subList(0, Math.min(duration * questionsPerDay, shuffled.size())));
        log.info("PLAN_QUESTIONS_ASSIGNED | planId={} | subject={} | duration={} | assigned={}",
            plan.getId(), plan.getSubject(), duration, Math.min(duration * questionsPerDay, shuffled.size()));
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

    /**
     * 调整计划题目（删除指定题目）
     * 只对 pending/active/paused 状态的计划开放
     */
    @Transactional
    public void adjustQuestions(UUID planId, List<UUID> removedQuestionIds) {
        LearningPlan plan = findById(planId);

        // completed 和 cancelled 状态不允许修改
        if ("completed".equals(plan.getStatus()) || "cancelled".equals(plan.getStatus())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_STATUS", "已完成或已取消的计划不能修改题目");
        }

        if (removedQuestionIds == null || removedQuestionIds.isEmpty()) {
            return;
        }

        // 删除指定的计划题目关联
        planQuestionRepository.deleteByPlanIdAndQuestionIdIn(planId, removedQuestionIds);

        log.info("PLAN_QUESTIONS_ADJUSTED | planId={} | removedCount={}", planId, removedQuestionIds.size());
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
