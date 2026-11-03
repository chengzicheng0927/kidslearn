package com.kidslearn.content.repository;

import com.kidslearn.content.domain.PlanQuestion;
import com.kidslearn.content.domain.QuizAnswer;
import com.kidslearn.report.domain.LearningPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PlanQuestionRepository extends JpaRepository<PlanQuestion, UUID> {
    List<PlanQuestion> findByPlanId(UUID planId);
    List<PlanQuestion> findByPlanIdIn(List<UUID> planIds);
    void deleteByPlanIdAndQuestionIdIn(UUID planId, List<UUID> questionIds);

    /**
     * 查询指定孩子已回答过的计划题目。
     * 通过 learning_plans(childId) → plan_questions → quiz_answers 三表关联。
     */
    @Query("""
        SELECT DISTINCT pq FROM PlanQuestion pq
        JOIN LearningPlan lp ON lp.id = pq.planId
        JOIN QuizAnswer qa ON qa.planQuestionId = pq.id
        WHERE lp.childId = :childId
        """)
    List<PlanQuestion> findByChildIdAnswered(@Param("childId") UUID childId);

    /**
     * 查询指定孩子所有计划关联的题目（不限是否已回答）。
     * 用于题库去重：排除已分配过的 question_bank 题目。
     */
    @Query("""
        SELECT pq FROM PlanQuestion pq
        JOIN LearningPlan lp ON lp.id = pq.planId
        WHERE lp.childId = :childId
        """)
    List<PlanQuestion> findByChildIdAll(@Param("childId") UUID childId);
}
