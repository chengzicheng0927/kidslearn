package com.kidslearn.content.repository;

import com.kidslearn.content.domain.WrongQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WrongQuestionRepository extends JpaRepository<WrongQuestion, UUID> {
    List<WrongQuestion> findByChildId(UUID childId);
    Optional<WrongQuestion> findByChildIdAndQuestionId(UUID childId, UUID questionId);
    boolean existsByChildIdAndQuestionId(UUID childId, UUID questionId);
}
