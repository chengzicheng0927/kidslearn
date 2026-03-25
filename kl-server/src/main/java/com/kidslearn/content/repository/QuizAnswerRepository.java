package com.kidslearn.content.repository;

import com.kidslearn.content.domain.QuizAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, UUID> {
}
