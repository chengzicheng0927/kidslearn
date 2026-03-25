package com.kidslearn.content.repository;

import com.kidslearn.content.domain.LearningStageDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LearningStageRepository extends JpaRepository<LearningStageDefinition, String> {

    List<LearningStageDefinition> findBySubject(String subject);

    @Query("SELECT s FROM LearningStageDefinition s WHERE s.subject = :subject AND s.minAge <= :age AND s.maxAge >= :age ORDER BY s.sortOrder")
    List<LearningStageDefinition> findBySubjectAndAgeRange(String subject, int age);

    List<LearningStageDefinition> findByIdIn(List<String> ids);
}
