package com.kidslearn.content.repository;

import com.kidslearn.content.domain.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface QuestionRepository extends JpaRepository<Question, UUID> {

    List<Question> findBySubjectAndDifficulty(String subject, String difficulty);

    @Query("SELECT q FROM Question q WHERE q.subject = :subject AND q.auditStatus = 'approved' ORDER BY FUNCTION('RANDOM')")
    List<Question> findRandomBySubject(String subject, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT q FROM Question q WHERE q.subject = :subject AND q.auditStatus = 'approved' " +
           "AND q.id NOT IN ( " +
           "  SELECT qa.questionId FROM QuizAnswer qa, LearningSession ls " +
           "  WHERE qa.sessionId = ls.id AND ls.childId = :childId " +
           ") ORDER BY FUNCTION('RANDOM')")
    List<Question> findUnansweredRandomBySubject(String subject, UUID childId, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT q FROM Question q WHERE q.topicKey = :topicKey AND q.auditStatus = 'approved' ORDER BY FUNCTION('RANDOM')")
    List<Question> findRandomByTopicKey(String topicKey, org.springframework.data.domain.Pageable pageable);

    /**
     * 查询指定日期内未答的题目（用于按计划日期取题）。
     * 按创建顺序返回，保证同一题目每次查询结果一致。
     */
    @Query("SELECT q FROM Question q WHERE q.subject = :subject " +
           "AND q.scheduledDate = :date AND q.auditStatus = 'approved' " +
           "AND q.id NOT IN ( " +
           "  SELECT qa.questionId FROM QuizAnswer qa, LearningSession ls " +
           "  WHERE qa.sessionId = ls.id AND ls.childId = :childId " +
           ") ORDER BY q.createdAt ASC")
    List<Question> findUnansweredByScheduledDate(String subject, LocalDate date, UUID childId);

    /**
     * 统计指定日期、指定学科的题目总数（用于今日任务展示）。
     */
    @Query("SELECT COUNT(q) FROM Question q WHERE q.subject = :subject " +
           "AND q.scheduledDate = :date AND q.auditStatus = 'approved'")
    long countByScheduledDateAndSubject(LocalDate date, String subject);

    /**
     * 查询指定日期的所有题目（用于今日任务星星累加）。
     */
    @Query("SELECT q FROM Question q WHERE q.scheduledDate = :date AND q.auditStatus = 'approved'")
    List<Question> findAllByScheduledDate(LocalDate date);
}
