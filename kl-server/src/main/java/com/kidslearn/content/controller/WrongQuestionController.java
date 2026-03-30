package com.kidslearn.content.controller;

import com.kidslearn.common.record.ApiResponse;
import com.kidslearn.content.domain.Question;
import com.kidslearn.content.domain.WrongQuestion;
import com.kidslearn.content.dto.WrongQuestionDTO;
import com.kidslearn.content.repository.QuestionRepository;
import com.kidslearn.content.repository.WrongQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/children/{childId}/wrong-questions")
@RequiredArgsConstructor
public class WrongQuestionController {

    private final WrongQuestionRepository wrongQuestionRepository;
    private final QuestionRepository questionRepository;

    @GetMapping
    public ApiResponse<List<WrongQuestionDTO>> getWrongQuestions(@PathVariable UUID childId) {
        // 只返回尚未达到巩固次数要求的错题
        List<WrongQuestion> wrongQuestions = wrongQuestionRepository.findByChildId(childId).stream()
            .filter(wq -> wq.getCorrectCount() < wq.getThreshold())
            .toList();
        
        List<WrongQuestionDTO> dtos = wrongQuestions.stream().map(wq -> {
            Question q = questionRepository.findById(wq.getQuestionId()).orElse(null);
            if (q == null) return null;

            List<WrongQuestionDTO.QuizOption> options = q.getOptions().stream()
                .map(opt -> new WrongQuestionDTO.QuizOption(
                    opt.get("id"), opt.get("text"), opt.getOrDefault("emoji", "")
                ))
                .toList();

            return new WrongQuestionDTO(
                wq.getId(),
                wq.getQuestionId(),
                q.getSubject(),
                q.getTopic(),
                q.getQuestion(),
                options,
                wq.getWrongAnswerId(),
                q.getCorrectAnswerId(),
                q.getExplanation(),
                wq.getAnsweredAt() != null ? wq.getAnsweredAt() : wq.getCreatedAt(),
                wq.getWrongCount(),
                wq.getCorrectCount(),
                wq.getThreshold()
            );
        })
        .filter(dto -> dto != null)
        .collect(Collectors.toList());

        return ApiResponse.ok(dtos);
    }
}
