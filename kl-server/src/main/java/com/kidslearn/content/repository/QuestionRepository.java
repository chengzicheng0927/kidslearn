package com.kidslearn.content.repository;

import com.kidslearn.content.domain.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
}
