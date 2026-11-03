package com.kidslearn.content.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidslearn.config.AppProperties;
import com.kidslearn.content.domain.BankGenerationTask;
import com.kidslearn.content.domain.PlanQuestion;
import com.kidslearn.content.domain.QuestionBank;
import com.kidslearn.content.domain.Question;
import com.kidslearn.content.dto.QuestionBankDTO;
import com.kidslearn.content.repository.*;
import com.kidslearn.llm.service.LlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * 题库预生成服务。
 *
 * 两种触发方式：
 * 1. 定时任务（每天凌晨） - 按水位线检查，不足则生成
 * 2. API 手动触发 - 管理员可随时触发生成
 *
 * 生成方式：异步 @Async，LLM 生成题目后批量入库。
 * 注意：@Async 需通过代理调用才生效，因此注入 ObjectProvider<QuestionBankService> 调用自身。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionBankService {

    private final QuestionBankRepository questionBankRepository;
    private final BankGenerationTaskRepository taskRepository;
    private final LearningStageRepository stageRepository;
    private final QuestionRepository questionRepository;
    private final PlanQuestionRepository planQuestionRepository;
    private final LlmService llmService;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;
    /** 注入自身代理，确保 @Async 通过 Spring AOP 代理调用生效 */
    private final ObjectProvider<QuestionBankService> selfProvider;

    // ========== 定时任务 ==========

    /**
     * 每天凌晨按水位线检查并生成题库。
     * 通过 unique index 确保同一 topic+difficulty 不会并发生成。
     */
    @Scheduled(cron = "${app.bank-generation.cron:0 0 3 * * ?}")
    public void scheduledBatchGeneration() {
        log.info("BANK_CRON_START");
        triggerBatchGeneration();
        log.info("BANK_CRON_END");
    }

    /**
     * 批量生成：遍历所有 learning_stage_definitions，检查水量，不足则串行生成。
     * 每个 topic+difficulty 生成完成后再执行下一个，避免并发打爆 LLM 服务。
     */
    public void triggerBatchGeneration() {
        var stages = stageRepository.findAll();
        int completed = 0;
        int total = stages.size() * 3;
        for (var stage : stages) {
            for (String difficulty : List.of("easy", "medium", "hard")) {
                UUID taskId = selfProvider.getObject().createTaskInNewTransaction(stage.getId(), difficulty, null);
                if (taskId != null) {
                    runGenerationSync(taskId);
                    completed++;
                    log.info("BANK_BATCH_PROGRESS | completed={}/{} | topicKey={} | difficulty={}",
                        completed, total, stage.getId(), difficulty);
                    try { Thread.sleep(SERIAL_INTERVAL_SECONDS * 1000L); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                }
            }
        }
        log.info("BANK_BATCH_COMPLETE | completed={}", completed);
    }

    // ========== 单个生成 ==========

    /**
     * 手动触发单个 topic 的题库生成（管理员 API 调用）。
     * 异步执行，API 立即返回 taskId。
     *
     * @return taskId 如果创建了新任务，否则 null（已有进行中任务）
     */
    public UUID triggerGeneration(String topicKey, String difficulty, Integer count) {
        UUID taskId = selfProvider.getObject().createTaskInNewTransaction(topicKey, difficulty, count);
        if (taskId == null) return null;

        // 通过代理调用 @Async 方法
        selfProvider.getObject().runAsyncGeneration(taskId);
        return taskId;
    }

    /**
     * 串行触发多个 topic 的题库生成（批量接口使用）。
     * 同步执行，等每个 topic 生成完成后再返回。
     *
     * @return 成功创建并执行的任务ID列表
     */
    /** 串行生成间隔（秒），避免 LLM 服务过载 */
    private static final int SERIAL_INTERVAL_SECONDS = 3;

    public List<UUID> triggerGenerationSerial(List<String> topicKeys, String difficulty, Integer count) {
        List<UUID> taskIds = new ArrayList<>();
        List<String> difficulties = difficulty != null && !difficulty.isBlank()
            ? List.of(difficulty)
            : List.of("easy", "medium", "hard");

        int total = topicKeys.size() * difficulties.size();
        int completed = 0;

        for (String topicKey : topicKeys) {
            for (String diff : difficulties) {
                UUID taskId = selfProvider.getObject().createTaskInNewTransaction(topicKey, diff, count);
                if (taskId != null) {
                    runGenerationSync(taskId);
                    taskIds.add(taskId);
                    // 每个任务完成后等待一段时间，避免打爆 LLM 服务
                    try { Thread.sleep(SERIAL_INTERVAL_SECONDS * 1000L); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                }
                completed++;
                log.info("BANK_SERIAL_PROGRESS | completed={}/{} | topicKey={} | difficulty={}", completed, total, topicKey, diff);
            }
        }
        return taskIds;
    }

    /**
     * 同步执行生成（不经过 @Async，直接调用）。
     */
    private void runGenerationSync(UUID taskId) {
        BankGenerationTask task = taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            log.error("BANK_TASK_NOT_FOUND | taskId={}", taskId);
            return;
        }
        try {
            task.setStatus("generating");
            taskRepository.save(task);
            executeGeneration(task);
        } catch (Exception e) {
            log.error("BANK_GENERATION_FAILED | taskId={} | topicKey={} | error={}",
                taskId, task.getTopicKey(), e.getMessage(), e);
            task.setStatus("failed");
            task.setErrorMessage(e.getMessage());
            task.setCompletedAt(Instant.now());
            taskRepository.save(task);
        }
    }

    /**
     * 在独立事务中创建生成任务（REQUIRES_NEW），确保提交后异步线程可见。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID createTaskInNewTransaction(String topicKey, String difficulty, Integer overrideCount) {
        // 1. 查水量
        long available = questionBankRepository.countAvailableByTopicKeyAndDifficulty(topicKey, difficulty);
        var props = appProperties.bankGeneration();
        int minWaterline = props.minWaterline();
        int defaultBatch = props.defaultBatchCount();

        if (available >= minWaterline) {
            log.debug("BANK_WATERLINE_OK | topicKey={} | difficulty={} | available={} | min={}",
                topicKey, difficulty, available, minWaterline);
            return null;
        }

        // 2. 尝试创建任务（unique index 防止并发生成）
        int targetCount = overrideCount != null ? overrideCount : defaultBatch;
        targetCount = Math.max(targetCount, 5);

        var stage = stageRepository.findById(topicKey).orElse(null);
        String taskGrade = stage != null ? estimateGrade(stage.getMinAge(), stage.getMaxAge()) : null;
        String taskSubject = stage != null ? stage.getSubject() : null;

        BankGenerationTask task = BankGenerationTask.builder()
            .topicKey(topicKey)
            .difficulty(difficulty)
            .grade(taskGrade)
            .subject(taskSubject)
            .targetCount(targetCount)
            .status("pending")
            .build();

        try {
            task = taskRepository.save(task);
        } catch (Exception e) {
            log.info("BANK_TASK_DUPLICATE_SKIP | topicKey={} | difficulty={} | reason=unique_index",
                topicKey, difficulty);
            return null;
        }

        return task.getId();
    }

    /**
     * 异步执行题库生成（@Async，不阻塞 cron）。
     */
    @Async
    public void runAsyncGeneration(UUID taskId) {
        BankGenerationTask task = taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            log.error("BANK_TASK_NOT_FOUND | taskId={}", taskId);
            return;
        }

        try {
            task.setStatus("generating");
            taskRepository.save(task);

            executeGeneration(task);
        } catch (Exception e) {
            log.error("BANK_GENERATION_FAILED | taskId={} | topicKey={} | error={}",
                taskId, task.getTopicKey(), e.getMessage(), e);
            task.setStatus("failed");
            task.setErrorMessage(e.getMessage());
            task.setCompletedAt(Instant.now());
            taskRepository.save(task);
        }
    }

    /** 每批最多生成的题目数（避免 LLM maxTokens 截断） */
    private static final int BATCH_SIZE = 10;

    private void executeGeneration(BankGenerationTask task) {
        String topicKey = task.getTopicKey();
        String difficulty = task.getDifficulty();
        int targetCount = task.getTargetCount();

        log.info("BANK_EXECUTE | taskId={} | topicKey={} | difficulty={} | count={}",
            task.getId(), topicKey, difficulty, targetCount);

        var stage = stageRepository.findById(topicKey).orElse(null);
        if (stage == null) {
            throw new RuntimeException("topic not found in learning_stage_definitions: " + topicKey);
        }

        String topicName = stage.getTopic();
        String subject = stage.getSubject();
        String grade = estimateGrade(stage.getMinAge(), stage.getMaxAge());

        // 分批调用 LLM（每批最多 BATCH_SIZE 道），避免 maxTokens 截断导致 JSON 不完整
        int totalSaved = 0;
        int remaining = targetCount;

        while (remaining > 0) {
            int batchCount = Math.min(remaining, BATCH_SIZE);
            try {
                List<Map<String, Object>> questions = llmService.generateBankQuestions(
                    topicKey, topicName, subject, difficulty, batchCount);

                int batchSaved = saveGeneratedQuestions(questions, topicKey, difficulty, topicName, subject, grade);
                totalSaved += batchSaved;
                remaining -= batchCount;

                log.info("BANK_BATCH_SAVED | taskId={} | batch={} | saved={} | remaining={}",
                    task.getId(), batchCount, batchSaved, remaining);
            } catch (Exception e) {
                log.warn("BANK_BATCH_ERROR | taskId={} | topicKey={} | remaining={} | error={}",
                    task.getId(), topicKey, remaining, e.getMessage());
                // 单批失败不中断整个任务，跳过本批继续
                remaining -= batchCount;
            }
        }

        task.setQuestionsGenerated(totalSaved);
        task.setStatus("completed");
        task.setCompletedAt(Instant.now());
        taskRepository.save(task);

        log.info("BANK_EXECUTE_COMPLETE | taskId={} | topicKey={} | saved={} | requested={}",
            task.getId(), topicKey, totalSaved, targetCount);
    }

    /**
     * 将 LLM 生成的题目保存到 question_bank，返回成功保存数量。
     */
    private int saveGeneratedQuestions(List<Map<String, Object>> questions, String topicKey,
                                       String difficulty, String topicName, String subject, String grade) {
        int saved = 0;
        for (Map<String, Object> q : questions) {
            try {
                String questionText = String.valueOf(q.getOrDefault("question", ""));
                if (questionBankRepository.existsByTopicKeyAndDifficultyAndQuestion(topicKey, difficulty, questionText)) {
                    log.debug("BANK_DUPLICATE_SKIP | topicKey={} | question={}", topicKey,
                        questionText.substring(0, Math.min(20, questionText.length())));
                    continue;
                }

                QuestionBank bank = QuestionBank.builder()
                    .grade(grade)
                    .subject(subject)
                    .topic(topicName)
                    .topicKey(topicKey)
                    .difficulty(difficulty)
                    .label(String.valueOf(q.getOrDefault("label", topicName)))
                    .question(questionText)
                    .options(objectMapper.convertValue(q.get("options"),
                        new TypeReference<List<Map<String, String>>>() {}))
                    .correctAnswerId(String.valueOf(q.getOrDefault("correctAnswerId", "a")))
                    .explanation(String.valueOf(q.getOrDefault("explanation", "")))
                    .explanationSteps(objectMapper.convertValue(q.get("explanationSteps"), String[].class))
                    .encouragement(String.valueOf(q.getOrDefault("encouragement", "太棒了！")))
                    .estimatedMinutes(3)
                    .reward(Map.of("stars", 8, "xp", 18))
                    .status("approved")
                    .generationModel("bank-generation")
                    .generatedAt(Instant.now())
                    .build();

                questionBankRepository.save(bank);
                saved++;
            } catch (Exception e) {
                log.warn("BANK_QUESTION_SAVE_ERROR | topicKey={} | error={}", topicKey, e.getMessage());
            }
        }
        return saved;
    }

    /**
     * 根据年龄估算年级范围。
     * 中国大陆学制：6岁一年级，7岁二年级，...
     */
    private String estimateGrade(int minAge, int maxAge) {
        // 取中间年龄估算
        int midAge = (minAge + maxAge) / 2;
        int gradeNum = midAge - 5; // 6岁 -> 一年级
        if (gradeNum < 1) gradeNum = 1;
        if (gradeNum > 6) gradeNum = 6;
        return gradeNum + "年级";
    }

    // ========== 计划激活时匹配题目 ==========

    /**
     * 从题库按 topic_key 精确匹配题目，分配到计划。
     *
     * @param planId  计划ID
     * @param childId 孩子ID（用于按学生维度排除已做过的题目）
     * @param grade   孩子年级（用于按年级匹配）
     * @param subject 科目
     * @param phases  计划阶段列表（每个 phase 需含 topicKey、difficulty、questionsPerDay）
     * @return 分配结果
     */
    @Transactional
    public QuestionBankDTO.AssignResult assignQuestionsFromBank(UUID planId, UUID childId, String grade, String subject,
                                        List<Map<String, Object>> phases) {
        log.info("BANK_ASSIGN | planId={} | childId={} | subject={} | grade={} | phases={}",
            planId, childId, subject, grade, phases.size());

        // 如果 plan 已有 questions（来自 generate_questions tool 的 LLM 实时生成），跳过
        List<PlanQuestion> existingPQs = planQuestionRepository.findByPlanId(planId);
        if (!existingPQs.isEmpty()) {
            log.info("BANK_ASSIGN_SKIP | planId={} | reason=already_has_questions | count={}",
                planId, existingPQs.size());
            return new QuestionBankDTO.AssignResult(0, 0, List.of(), true, "计划已有题目");
        }

        // 查询这个孩子在所有计划中已使用过的 question_bank ID（按学生维度排除）
        Set<UUID> usedBankIds = getUsedBankQuestionIds(childId);

        int totalAssigned = 0;
        Set<String> insufficientTopicKeys = new LinkedHashSet<>();

        for (int i = 0; i < phases.size(); i++) {
            Map<String, Object> phase = phases.get(i);
            String topicKey = String.valueOf(phase.getOrDefault("topicKey", phase.get("topic")));
            String difficulty = String.valueOf(phase.getOrDefault("difficulty", "medium"));
            int questionsPerDay = ((Number) phase.getOrDefault("questionsPerDay", 10)).intValue();

            // 先查 grade 精确匹配
            List<QuestionBank> pool = questionBankRepository.findAvailableByTopicKeyAndDifficultyAndGrade(
                topicKey, difficulty, grade,
                org.springframework.data.domain.PageRequest.of(0, questionsPerDay * 2));

            // 不足时 fallback 到不限年级
            if (pool.size() < questionsPerDay) {
                List<QuestionBank> extra = questionBankRepository.findAvailableByTopicKeyAndDifficulty(
                    topicKey, difficulty, null,
                    org.springframework.data.domain.PageRequest.of(0, questionsPerDay * 2 - pool.size()));
                for (QuestionBank qb : extra) {
                    if (!pool.contains(qb)) pool.add(qb);
                }
            }

            if (pool.isEmpty()) {
                log.warn("BANK_INSUFFICIENT | planId={} | topicKey={} | difficulty={} | required={} | found=0",
                    planId, topicKey, difficulty, questionsPerDay);
                insufficientTopicKeys.add(topicKey);
                continue;
            }

            // 打乱顺序（用 Collections.shuffle 需要 List）
            List<QuestionBank> shuffled = new ArrayList<>(pool);
            Collections.shuffle(shuffled);
            List<QuestionBank> selected = shuffled.subList(0, Math.min(questionsPerDay * 2, shuffled.size()));

            // 过滤已使用过的题库题目（按 question_bank.id 排除）
            List<QuestionBank> toAssign = selected.stream()
                .filter(qb -> !usedBankIds.contains(qb.getId()))
                .toList();

            if (toAssign.isEmpty()) {
                log.warn("BANK_ALL_ANSWERED | planId={} | topicKey={}", planId, topicKey);
                insufficientTopicKeys.add(topicKey);
                continue;
            }

            // 批量保存到 questions 表 + plan_questions 关联
            for (QuestionBank bank : toAssign) {
                Question q = Question.builder()
                    .subject(bank.getSubject())
                    .topic(bank.getTopic())
                    .topicKey(bank.getTopicKey())
                    .difficulty(bank.getDifficulty())
                    .label(bank.getLabel())
                    .question(bank.getQuestion())
                    .options(bank.getOptions())
                    .correctAnswerId(bank.getCorrectAnswerId())
                    .explanation(bank.getExplanation())
                    .explanationSteps(bank.getExplanationSteps())
                    .encouragement(bank.getEncouragement())
                    .estimatedMinutes(bank.getEstimatedMinutes())
                    .reward(bank.getReward())
                    .auditStatus("approved")
                    .generationModel("bank:" + bank.getId())
                    .build();
                q = questionRepository.save(q);

                PlanQuestion pq = PlanQuestion.builder()
                    .planId(planId)
                    .questionId(q.getId())
                    .phaseIndex(i)
                    .purpose("practice")
                    .build();
                planQuestionRepository.save(pq);
                totalAssigned++;
            }

            log.info("BANK_PHASE_ASSIGNED | planId={} | phaseIndex={} | topicKey={} | assigned={}",
                planId, i, topicKey, toAssign.size());
        }

        log.info("BANK_ASSIGN_COMPLETE | planId={} | totalAssigned={}", planId, totalAssigned);

        // 构建结果消息
        String message;
        boolean success = totalAssigned > 0;
        if (insufficientTopicKeys.isEmpty()) {
            message = "题库匹配成功，共分配 " + totalAssigned + " 道题目";
        } else if (totalAssigned > 0) {
            message = "题库部分匹配，成功分配 " + totalAssigned + " 道题目，但以下 topic 题目不足：" + String.join("、", insufficientTopicKeys);
        } else {
            message = "题库题目不足，以下 topic 没有可用题目：" + String.join("、", insufficientTopicKeys) + "。请联系管理员补充题库。";
        }

        return new QuestionBankDTO.AssignResult(totalAssigned, insufficientTopicKeys.size(), new ArrayList<>(insufficientTopicKeys), success, message);
    }

    /**
     * 获取孩子在所有计划中已使用过的 question_bank ID 集合。
     * 通过 questions 表的 generationModel 字段（格式 "bank:<bankId>"）提取。
     */
    private Set<UUID> getUsedBankQuestionIds(UUID childId) {
        // 查询这个 child 的所有计划关联的 question_id
        List<PlanQuestion> allPqs = planQuestionRepository.findByChildIdAll(childId);
        Set<UUID> questionIds = allPqs.stream()
            .map(PlanQuestion::getQuestionId)
            .collect(java.util.stream.Collectors.toSet());

        if (questionIds.isEmpty()) return Set.of();

        // 从 questions 表中查 generationModel 以 "bank:" 开头的记录，提取 bank UUID
        return questionRepository.findAllById(questionIds).stream()
            .filter(q -> q.getGenerationModel() != null && q.getGenerationModel().startsWith("bank:"))
            .map(q -> {
                try {
                    return UUID.fromString(q.getGenerationModel().substring(5));
                } catch (IllegalArgumentException e) {
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(java.util.stream.Collectors.toSet());
    }
}
