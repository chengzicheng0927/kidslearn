package com.kidslearn.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidslearn.chat.domain.ChatConversation;
import com.kidslearn.chat.repository.ChatConversationRepository;
import com.kidslearn.llm.dto.LlmRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatHistoryService {

    private final ChatConversationRepository repository;
    private final ObjectMapper objectMapper;

    @Value("${app.chat.history-max-size:10}")
    private int historyMaxSize;

    @Value("${app.chat.history-expire-days:7}")
    private int historyExpireDays;

    /**
     * 获取对话历史，转换为 LLM 消息格式
     */
    @Transactional(readOnly = true)
    public List<LlmRequest.Message> getHistoryMessages(UUID parentId, UUID childId) {
        List<ChatConversation> conversations;

        if (childId != null) {
            conversations = repository.findRecentByParentIdAndChildId(parentId, childId, historyMaxSize);
        } else {
            conversations = repository.findRecentByParentId(parentId, historyMaxSize);
        }

        // 反转顺序（从旧到新）
        Collections.reverse(conversations);

        List<LlmRequest.Message> messages = new ArrayList<>();
        for (ChatConversation conv : conversations) {
            if ("user".equals(conv.getRole())) {
                messages.add(new LlmRequest.Message("user", conv.getContent()));
            } else if ("assistant".equals(conv.getRole())) {
                messages.add(new LlmRequest.Message("assistant", conv.getContent()));
            }
        }

        return messages;
    }

    /**
     * 保存用户消息
     */
    @Transactional
    public void saveUserMessage(UUID parentId, UUID childId, String content) {
        ChatConversation conv = ChatConversation.builder()
            .parentId(parentId)
            .childId(childId)
            .role("user")
            .content(content)
            .build();
        repository.save(conv);
        log.info("CHAT_HISTORY_SAVE | role=user | parentId={} | childId={}", parentId, childId);
    }

    /**
     * 保存助手消息（LLM 回复）
     */
    @Transactional
    public void saveAssistantMessage(UUID parentId, UUID childId, String content) {
        ChatConversation conv = ChatConversation.builder()
            .parentId(parentId)
            .childId(childId)
            .role("assistant")
            .content(content)
            .build();
        repository.save(conv);
        log.info("CHAT_HISTORY_SAVE | role=assistant | parentId={} | childId={}", parentId, childId);
    }

    /**
     * 保存助手消息（包含 tool 调用）
     */
    @Transactional
    public void saveAssistantMessageWithTool(UUID parentId, UUID childId,
            String content, List<Object> toolCalls, Object toolResults) {
        ChatConversation conv = ChatConversation.builder()
            .parentId(parentId)
            .childId(childId)
            .role("assistant")
            .content(content)
            .toolCalls(toolCalls)
            .toolResults(toolResults)
            .build();
        repository.save(conv);
        log.info("CHAT_HISTORY_SAVE | role=assistant | parentId={} | childId={} | hasToolCalls={}",
            parentId, childId, toolCalls != null && !toolCalls.isEmpty());
    }

    /**
     * 清理过期对话历史
     */
    @Transactional
    public int cleanExpiredHistory() {
        Instant before = Instant.now().minus(historyExpireDays, ChronoUnit.DAYS);
        int deleted = repository.deleteOldRecords(before);
        log.info("CHAT_HISTORY_CLEAN | deleted={} | before={}", deleted, before);
        return deleted;
    }
}
