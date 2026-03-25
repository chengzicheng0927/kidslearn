package com.kidslearn.chat.controller;

import com.kidslearn.chat.dto.*;
import com.kidslearn.chat.service.SafetyFilterService;
import com.kidslearn.common.exception.BusinessException;
import com.kidslearn.common.record.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/parent/chat")
@RequiredArgsConstructor
@Slf4j
public class ParentChatController {

    private final SafetyFilterService safetyFilterService;

    @PostMapping
    public ApiResponse<ParentChatResponse> chat(@Valid @RequestBody ParentChatRequest req,
                                                 Authentication auth) {
        UUID parentId = (UUID) auth.getPrincipal();

        // 安全过滤
        var filterResult = safetyFilterService.filterInput(req.message(), parentId);
        if (filterResult.outcome() != SafetyFilterService.FilterResult.Outcome.PASS) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "SAFETY_FILTER", filterResult.reason());
        }

        // TODO: LLM 集成
        var response = new ParentChatResponse(
            "收到您的消息: " + req.message() + "。LLM 集成即将上线，敬请期待！",
            "text", null, null, null, false
        );

        return ApiResponse.ok(response);
    }
}
