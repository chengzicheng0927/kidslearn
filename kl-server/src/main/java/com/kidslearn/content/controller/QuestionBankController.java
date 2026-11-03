package com.kidslearn.content.controller;

import com.kidslearn.common.record.ApiResponse;
import com.kidslearn.content.domain.BankGenerationTask;
import com.kidslearn.content.domain.LearningStageDefinition;
import com.kidslearn.content.dto.QuestionBankDTO;
import com.kidslearn.content.repository.BankGenerationTaskRepository;
import com.kidslearn.content.repository.LearningStageRepository;
import com.kidslearn.content.repository.QuestionBankRepository;
import com.kidslearn.content.service.QuestionBankService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

/**
 * 题库管理 API（管理员用）
 */
@RestController
@RequestMapping("/api/admin/question-bank")
@RequiredArgsConstructor
public class QuestionBankController {

    private final QuestionBankService questionBankService;
    private final QuestionBankRepository questionBankRepository;
    private final BankGenerationTaskRepository taskRepository;
    private final LearningStageRepository stageRepository;

    /**
     * 查询题库统计（按 topic 分组，支持 subject 过滤）
     */
    @GetMapping("/stats")
    public ApiResponse<QuestionBankDTO.BankStatsResponse> getStats(
            @RequestParam(required = false) String subject) {
        List<LearningStageDefinition> stages = stageRepository.findAll();

        // 按 subject 过滤
        if (subject != null && !subject.isBlank()) {
            stages = stages.stream()
                .filter(s -> subject.equals(s.getSubject()))
                .toList();
        }

        // 按 topicKey + status 分组统计
        Map<String, Map<String, Long>> statusCountsByTopic = new HashMap<>();
        for (Object[] row : questionBankRepository.countByTopicKeyAndStatus()) {
            String tk = (String) row[0];
            String status = (String) row[1];
            long count = (Long) row[2];
            statusCountsByTopic.computeIfAbsent(tk, k -> new HashMap<>()).put(status, count);
        }

        // 查询各 topic 的最新生成时间
        Map<String, Instant> lastGeneratedByTopic = new HashMap<>();
        for (var s : stages) {
            List<BankGenerationTask> tasks = taskRepository.findByTopicKeyOrderByCreatedAtDesc(s.getId());
            if (!tasks.isEmpty()) {
                lastGeneratedByTopic.put(s.getId(), tasks.get(0).getCompletedAt());
            }
        }

        List<QuestionBankDTO.TopicStats> topicStats = stages.stream()
            .map(s -> {
                Map<String, Long> sc = statusCountsByTopic.getOrDefault(s.getId(), Map.of());
                long approved = sc.getOrDefault("approved", 0L);
                long generating = sc.getOrDefault("generating", 0L);
                long failed = sc.getOrDefault("failed", 0L);
                return new QuestionBankDTO.TopicStats(
                    s.getId(),
                    s.getTopic(),
                    s.getSubject(),
                    approved + generating + failed,
                    approved,
                    generating,
                    failed,
                    lastGeneratedByTopic.get(s.getId())
                );
            })
            .toList();

        return ApiResponse.ok(new QuestionBankDTO.BankStatsResponse(topicStats));
    }

    /**
     * 触发生成题库。
     * - 传 topicKey：异步生成该 topic，API 立即返回
     * - 不传 topicKey：串行遍历所有 topic，逐个生成完成后返回
     * - difficulty 不传时遍历 easy/medium/hard
     */
    @PostMapping("/generate")
    public ApiResponse<?> triggerGenerate(@Valid @RequestBody GenerateRequest req) {
        // 指定 topicKey → 异步生成（API 立即返回）
        if (req.topicKey() != null && !req.topicKey().isBlank()) {
            List<String> difficulties = req.difficulty() != null && !req.difficulty().isBlank()
                ? List.of(req.difficulty())
                : List.of("easy", "medium", "hard");

            List<QuestionBankDTO.GenerateResponse> results = new ArrayList<>();
            for (String diff : difficulties) {
                UUID taskId = questionBankService.triggerGeneration(req.topicKey(), diff, req.count());
                results.add(taskId != null
                    ? new QuestionBankDTO.GenerateResponse(taskId, "generating", req.topicKey() + "/" + diff)
                    : new QuestionBankDTO.GenerateResponse(null, "skipped", req.topicKey() + "/" + diff + " 水量充足或已有任务"));
            }
            return ApiResponse.ok(results);
        }

        // 未指定 topicKey → 串行遍历所有 topic，逐个生成
        List<String> allTopicKeys = stageRepository.findAll().stream()
            .map(s -> s.getId())
            .toList();

        List<UUID> taskIds = questionBankService.triggerGenerationSerial(
            allTopicKeys, req.difficulty(), req.count());

        String message = taskIds.isEmpty()
            ? "所有 topic 水量充足，无需生成"
            : "串行生成完成，共执行 " + taskIds.size() + " 个任务";
        return ApiResponse.ok(Map.of("message", message, "taskCount", taskIds.size()));
    }

    /**
     * 查询生成任务状态
     */
    @GetMapping("/tasks/{taskId}")
    public ApiResponse<QuestionBankDTO.TaskInfo> getTask(@PathVariable UUID taskId) {
        BankGenerationTask task = taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            return ApiResponse.ok(null);
        }
        return ApiResponse.ok(new QuestionBankDTO.TaskInfo(
            task.getId(),
            task.getTopicKey(),
            task.getDifficulty(),
            task.getTargetCount(),
            task.getQuestionsGenerated(),
            task.getStatus(),
            task.getErrorMessage(),
            task.getCreatedAt(),
            task.getCompletedAt()
        ));
    }

    /**
     * 批量触发生成（按 topic 列表）
     */
    @PostMapping("/generate/batch")
    public ApiResponse<Void> triggerBatch() {
        questionBankService.triggerBatchGeneration();
        return ApiResponse.ok(null);
    }

    public record GenerateRequest(
        String topicKey,         // 不传则遍历所有 topic
        String difficulty,       // 不传则遍历 easy/medium/hard
        @Min(1) @Max(100) Integer count  // 不传则使用 defaultBatchCount
    ) {}
}
