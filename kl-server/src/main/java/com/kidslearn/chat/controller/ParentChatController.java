package com.kidslearn.chat.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidslearn.chat.dto.*;
import com.kidslearn.chat.service.ChatHistoryService;
import com.kidslearn.chat.service.SafetyFilterService;
import com.kidslearn.common.exception.BusinessException;
import com.kidslearn.common.record.ApiResponse;
import com.kidslearn.llm.dto.LlmRequest;
import com.kidslearn.user.domain.Child;
import com.kidslearn.user.repository.ChildRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import org.slf4j.MDC;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/parent/chat")
@RequiredArgsConstructor
@Slf4j
public class ParentChatController {

    private final SafetyFilterService safetyFilterService;
    private final com.kidslearn.llm.service.LlmService llmService;
    private final ChildRepository childRepository;
    private final ChatHistoryService chatHistoryService;
    private final ObjectMapper objectMapper;

    @PostMapping
    public ApiResponse<ParentChatResponse> chat(@Valid @RequestBody ParentChatRequest req,
                                                 Authentication auth) {
        UUID parentId = (UUID) auth.getPrincipal();
        UUID childId = req.context() != null ? req.context().childId() : null;
        String requestId = UUID.randomUUID().toString().substring(0, 8);

        try {
            MDC.put("requestId", requestId);
            MDC.put("parentId", parentId.toString());
            if (childId != null) MDC.put("childId", childId.toString());

            log.info("CHAT_START | message='{}' | childId={} | context={}", req.message(), childId, req.context());

            // 安全过滤
            var filterResult = safetyFilterService.filterInput(req.message(), parentId);
            if (filterResult.outcome() != SafetyFilterService.FilterResult.Outcome.PASS) {
                log.warn("CHAT_SAFETY_BLOCK | outcome={} | reason={}", filterResult.outcome(), filterResult.reason());
                throw new BusinessException(HttpStatus.BAD_REQUEST, "SAFETY_FILTER", filterResult.reason());
            }

            // 获取儿童画像用于 LLM 上下文
            // 优先使用请求中的 childId，否则根据 parentId 查询第一个孩子
            Child child = null;
            if (childId != null) {
                child = childRepository.findById(childId).orElse(null);
            }
            // 如果没找到，尝试根据 parentId 查询孩子列表
            if (child == null) {
                var children = childRepository.findByParentId(parentId);
                if (!children.isEmpty()) {
                    child = children.get(0); // 取第一个
                    childId = child.getId();
                    log.info("CHAT_CHILD_AUTO_SELECTED | childId={} | name={} | age={} | grade={}",
                        child.getId(), child.getName(), child.getAge(), child.getGrade());
                }
            }

            String childName = child != null ? child.getName() : "孩子";
            int childAge = child != null ? child.getAge() : 0;
            String childAgeGroup = child != null ? child.getAgeGroup() : (childAge > 0 ? Child.calcAgeGroup(childAge) : "未知");
            String childGrade = child != null && child.getGrade() != null ? child.getGrade() : "未知";

            log.info("CHAT_CONTEXT | childName={} | childAge={} | childGrade={} | childId={}",
                childName, childAge, childGrade, childId);

            com.kidslearn.llm.tools.ToolContext ctx = new com.kidslearn.llm.tools.ToolContext(
                parentId, childId, childName, childAge, childAgeGroup, childGrade,
                req.context() != null ? req.context().subject() : "en"
            );

            // 获取对话历史
            List<LlmRequest.Message> historyMessages = chatHistoryService.getHistoryMessages(parentId, childId);

            // 保存用户消息
            chatHistoryService.saveUserMessage(parentId, childId, req.message());

            // 调用 LLM 服务
            ParentChatResponse response = llmService.chat(req, ctx, historyMessages);

            // 保存助手回复
            chatHistoryService.saveAssistantMessage(parentId, childId, response.reply());

            log.info("CHAT_END | reply='{}'", response.reply());
            return ApiResponse.ok(response);
        } catch (BusinessException e) {
            log.warn("CHAT_BUSINESS_ERROR | code={} | message={}", e.getCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("CHAT_UNEXPECTED_ERROR | error={} | class={}", e.getMessage(), e.getClass().getSimpleName(), e);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "服务暂时不可用");
        } finally {
            MDC.clear();
        }
    }
}
