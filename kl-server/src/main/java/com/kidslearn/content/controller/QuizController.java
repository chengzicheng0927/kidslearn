package com.kidslearn.content.controller;

import com.kidslearn.common.record.ApiResponse;
import com.kidslearn.content.dto.*;
import com.kidslearn.content.service.QuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @PostMapping("/next")
    public ApiResponse<QuizNextResponse> next(@RequestBody QuizNextRequest req,
                                              @RequestParam UUID childId) {
        return ApiResponse.ok(quizService.getNextQuestion(req, childId));
    }

    @PostMapping("/submit")
    public ApiResponse<QuizSubmitResponse> submit(@Valid @RequestBody QuizSubmitRequest req,
                                                   @RequestParam UUID childId) {
        return ApiResponse.ok(quizService.submitAnswer(req, childId));
    }

    @PostMapping("/reset")
    public ApiResponse<Void> reset(@RequestParam UUID childId) {
        // Reset logic - end current session
        return ApiResponse.ok();
    }
}
