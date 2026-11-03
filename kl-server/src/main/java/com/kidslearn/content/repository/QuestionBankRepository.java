package com.kidslearn.content.repository;

import com.kidslearn.content.domain.QuestionBank;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuestionBankRepository extends JpaRepository<QuestionBank, UUID> {

    /**
     * 按 topic_key + difficulty 查询可用题目（激活计划时用到）。
     * excludeQuestionIds 用于排除该学生在所有计划中已做过的题目。
     */
    @Query("""
        SELECT qb FROM QuestionBank qb
        WHERE qb.topicKey = :topicKey
          AND qb.difficulty = :difficulty
          AND qb.status = 'approved'
          AND (:excludeQuestionIds IS NULL OR qb.id NOT IN :excludeQuestionIds)
        ORDER BY FUNCTION('RANDOM')
        """)
    List<QuestionBank> findAvailableByTopicKeyAndDifficulty(
            @Param("topicKey") String topicKey,
            @Param("difficulty") String difficulty,
            @Param("excludeQuestionIds") List<UUID> excludeQuestionIds,
            Pageable pageable);

    /**
     * 统计指定 topic_key + difficulty 的可用题目数量。
     */
    @Query("SELECT COUNT(qb) FROM QuestionBank qb WHERE qb.topicKey = :topicKey AND qb.difficulty = :difficulty AND qb.status = 'approved'")
    long countAvailableByTopicKeyAndDifficulty(@Param("topicKey") String topicKey, @Param("difficulty") String difficulty);

    /**
     * 统计每个 difficulty 的可用题目数（用于水位检查）。
     * 返回 [topicKey, difficulty, count]
     */
    @Query("SELECT qb.topicKey, qb.difficulty, COUNT(qb) FROM QuestionBank qb WHERE qb.status = 'approved' GROUP BY qb.topicKey, qb.difficulty")
    List<Object[]> countByTopicKeyAndDifficulty();

    /**
     * 按 topicKey + status 分组统计（用于 stats 接口）。
     * 返回 [topicKey, status, count]
     */
    @Query("SELECT qb.topicKey, qb.status, COUNT(qb) FROM QuestionBank qb GROUP BY qb.topicKey, qb.status")
    List<Object[]> countByTopicKeyAndStatus();

    /**
     * 查询指定 topic_key + difficulty + grade 的可用题目。
     * 用于精确匹配孩子的年级。
     */
    @Query("""
        SELECT qb FROM QuestionBank qb
        WHERE qb.topicKey = :topicKey
          AND qb.difficulty = :difficulty
          AND qb.grade = :grade
          AND qb.status = 'approved'
        ORDER BY FUNCTION('RANDOM')
        """)
    List<QuestionBank> findAvailableByTopicKeyAndDifficultyAndGrade(
            @Param("topicKey") String topicKey,
            @Param("difficulty") String difficulty,
            @Param("grade") String grade,
            Pageable pageable);

    /**
     * 批量更新状态（生成完成后调用）。
     */
    @Modifying
    @Query("UPDATE QuestionBank qb SET qb.status = :status WHERE qb.id IN :ids")
    int updateStatusByIds(@Param("ids") List<UUID> ids, @Param("status") String status);

    /**
     * 检查题目是否已存在（去重）。
     */
    boolean existsByTopicKeyAndDifficultyAndQuestion(String topicKey, String difficulty, String question);
}
