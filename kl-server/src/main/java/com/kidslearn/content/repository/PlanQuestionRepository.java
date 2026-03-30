package com.kidslearn.content.repository;

import com.kidslearn.content.domain.PlanQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PlanQuestionRepository extends JpaRepository<PlanQuestion, UUID> {
    List<PlanQuestion> findByPlanId(UUID planId);
    void deleteByPlanIdAndQuestionIdIn(UUID planId, List<UUID> questionIds);
}
