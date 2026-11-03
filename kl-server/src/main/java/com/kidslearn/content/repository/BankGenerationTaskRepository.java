package com.kidslearn.content.repository;

import com.kidslearn.content.domain.BankGenerationTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BankGenerationTaskRepository extends JpaRepository<BankGenerationTask, UUID> {

    /**
     * 查询进行中或待处理的任务（用于 cron 重试）。
     */
    List<BankGenerationTask> findByStatusIn(List<String> statuses);

    /**
     * 查询指定 topic_key + difficulty 的进行中任务（防止并发生成）。
     */
    @Query("SELECT t FROM BankGenerationTask t WHERE t.topicKey = :topicKey AND t.difficulty = :difficulty AND t.status IN ('pending', 'generating')")
    Optional<BankGenerationTask> findActiveByTopicKeyAndDifficulty(
            @Param("topicKey") String topicKey,
            @Param("difficulty") String difficulty);

    /**
     * 查找最近完成的任务（用于查看某个 topic 的生成历史）。
     */
    @Query("SELECT t FROM BankGenerationTask t WHERE t.topicKey = :topicKey ORDER BY t.createdAt DESC")
    List<BankGenerationTask> findByTopicKeyOrderByCreatedAtDesc(@Param("topicKey") String topicKey);
}
