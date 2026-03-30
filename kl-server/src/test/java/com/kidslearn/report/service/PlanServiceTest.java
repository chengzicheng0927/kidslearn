package com.kidslearn.report.service;

import com.kidslearn.common.exception.BusinessException;
import com.kidslearn.content.domain.PlanQuestion;
import com.kidslearn.content.domain.Question;
import com.kidslearn.content.repository.PlanQuestionRepository;
import com.kidslearn.content.repository.QuestionRepository;
import com.kidslearn.report.domain.LearningPlan;
import com.kidslearn.report.dto.PlanDetailDTO;
import com.kidslearn.report.repository.LearningPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

    @Mock private LearningPlanRepository planRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private PlanQuestionRepository planQuestionRepository;

    @InjectMocks
    private PlanService planService;

    private final UUID planId = UUID.randomUUID();
    private final UUID childId = UUID.randomUUID();

    private LearningPlan mockPlan(String status) {
        return LearningPlan.builder()
            .id(planId)
            .childId(childId)
            .subject("en")
            .status(status)
            .durationDays(14)
            .difficulty("medium")
            .phases(List.of())
            .build();
    }

    private Question mockQuestion(int idx) {
        Question q = new Question();
        q.setId(UUID.randomUUID());
        q.setSubject("en");
        q.setScheduledDate(null);
        q.setAuditStatus("approved");
        return q;
    }

    @Test
    void confirmPlan_assignsQuestionsToDates() {
        // Arrange
        LearningPlan plan = mockPlan("pending");
        when(planRepository.findById(planId)).thenReturn(java.util.Optional.of(plan));
        when(planRepository.save(any(LearningPlan.class))).thenAnswer(i -> i.getArgument(0));

        List<Question> pool = new ArrayList<>();
        for (int i = 0; i < 28; i++) pool.add(mockQuestion(i));
        when(questionRepository.findBySubjectAndDifficulty("en", "medium")).thenReturn(pool);
        when(questionRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        // Act
        LearningPlan result = planService.confirmPlan(planId);

        // Assert
        assertEquals("active", result.getStatus());
        assertNotNull(result.getStartedAt());

        // 验证 saveAll 被调用（题目分配）
        verify(questionRepository, atLeastOnce()).saveAll(anyList());

        // 验证至少有题目被分配了日期
        ArgumentCaptor<List<Question>> captor = ArgumentCaptor.forClass(List.class);
        verify(questionRepository).saveAll(captor.capture());
        List<Question> saved = captor.getValue();
        if (!saved.isEmpty()) {
            // 每个被保存的题目应有 scheduledDate
            for (Question q : saved) {
                assertNotNull(q.getScheduledDate());
            }
            // scheduledDate 应在今天到今天+14天之间
            LocalDate today = LocalDate.now();
            for (Question q : saved) {
                assertTrue(!q.getScheduledDate().isBefore(today));
                assertTrue(q.getScheduledDate().isBefore(today.plusDays(14)));
            }
        }
    }

    @Test
    void confirmPlan_alreadyActive_throwsException() {
        // Arrange
        LearningPlan plan = mockPlan("active");
        when(planRepository.findById(planId)).thenReturn(java.util.Optional.of(plan));

        // Act & Assert
        assertThrows(BusinessException.class, () -> planService.confirmPlan(planId));
    }

    @Test
    void confirmPlan_noQuestionsAvailable_stillSucceeds() {
        // Arrange
        LearningPlan plan = mockPlan("pending");
        when(planRepository.findById(planId)).thenReturn(java.util.Optional.of(plan));
        when(planRepository.save(any(LearningPlan.class))).thenAnswer(i -> i.getArgument(0));
        when(questionRepository.findBySubjectAndDifficulty("en", "medium")).thenReturn(List.of());

        // Act
        LearningPlan result = planService.confirmPlan(planId);

        // Assert: 不抛异常，状态变为 active
        assertEquals("active", result.getStatus());
        // saveAll 不应被调用（没有题目可分配）
        verify(questionRepository, never()).saveAll(anyList());
    }

    // ========== getPlanDetail Tests ==========

    @Test
    void getPlanDetail_completedPlan_returnsAccuracyAndAnswer() {
        // Arrange
        LearningPlan plan = LearningPlan.builder()
            .id(planId)
            .childId(childId)
            .subject("en")
            .status("completed")
            .durationDays(7)
            .difficulty("easy")
            .phases(List.of(
                Map.of("dayRange", "1-3", "goal", "学习颜色", "topic", "颜色", "questionsPerDay", 5, "difficulty", "easy")
            ))
            .totalCompleted(35)
            .totalCorrect(30)
            .startedAt(Instant.parse("2026-03-10T00:00:00Z"))
            .completedAt(Instant.parse("2026-03-17T00:00:00Z"))
            .build();

        Question question = new Question();
        question.setId(UUID.randomUUID());
        question.setSubject("en");
        question.setTopic("颜色");
        question.setQuestion("What color is the sky?");
        question.setOptions(List.of(
            Map.of("id", "a", "text", "Red"),
            Map.of("id", "b", "text", "Blue"),
            Map.of("id", "c", "text", "Green")
        ));
        question.setCorrectAnswerId("b");

        PlanQuestion planQuestion = PlanQuestion.builder()
            .id(UUID.randomUUID())
            .planId(planId)
            .questionId(question.getId())
            .phaseIndex(0)
            .addedAt(Instant.now())
            .build();

        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(planQuestionRepository.findByPlanId(planId)).thenReturn(List.of(planQuestion));
        when(questionRepository.findById(question.getId())).thenReturn(Optional.of(question));

        // Act
        PlanDetailDTO result = planService.getPlanDetail(planId);

        // Assert
        assertEquals(planId, result.id());
        assertEquals("en", result.subject());
        assertEquals("completed", result.status());
        assertEquals(35, result.totalCompleted());
        assertEquals(30, result.totalCorrect());
        assertEquals(85.7, result.accuracy(), 0.1);

        // 验证题目包含正确答案
        assertEquals(1, result.phases().size());
        assertEquals(1, result.phases().get(0).questions().size());
        assertEquals("Blue", result.phases().get(0).questions().get(0).answer());
    }

    @Test
    void getPlanDetail_activePlan_noAnswer() {
        // Arrange
        LearningPlan plan = LearningPlan.builder()
            .id(planId)
            .childId(childId)
            .subject("ma")
            .status("active")
            .durationDays(14)
            .difficulty("medium")
            .phases(List.of(
                Map.of("dayRange", "1-7", "goal", "加法", "topic", "加法", "questionsPerDay", 5, "difficulty", "easy")
            ))
            .totalCompleted(0)
            .totalCorrect(0)
            .startedAt(Instant.parse("2026-03-25T00:00:00Z"))
            .build();

        Question question = new Question();
        question.setId(UUID.randomUUID());
        question.setSubject("ma");
        question.setTopic("加法");
        question.setQuestion("5 + 3 = ?");
        question.setOptions(List.of(
            Map.of("id", "a", "text", "6"),
            Map.of("id", "b", "text", "7"),
            Map.of("id", "c", "text", "8")
        ));
        question.setCorrectAnswerId("c");

        PlanQuestion planQuestion = PlanQuestion.builder()
            .id(UUID.randomUUID())
            .planId(planId)
            .questionId(question.getId())
            .phaseIndex(0)
            .addedAt(Instant.now())
            .build();

        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(planQuestionRepository.findByPlanId(planId)).thenReturn(List.of(planQuestion));
        when(questionRepository.findById(question.getId())).thenReturn(Optional.of(question));

        // Act
        PlanDetailDTO result = planService.getPlanDetail(planId);

        // Assert
        assertEquals("active", result.status());
        assertEquals(0, result.accuracy(), 0.0); // 未完成无正确率

        // 验证题目不包含正确答案（active 计划不返回答案）
        assertNull(result.phases().get(0).questions().get(0).answer());
    }

    // ========== adjustQuestions Tests ==========

    @Test
    void adjustQuestions_pendingPlan_success() {
        // Arrange
        LearningPlan plan = LearningPlan.builder()
            .id(planId)
            .childId(childId)
            .status("pending")
            .build();
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));

        UUID questionId = UUID.randomUUID();

        // Act
        planService.adjustQuestions(planId, List.of(questionId));

        // Assert
        verify(planQuestionRepository).deleteByPlanIdAndQuestionIdIn(planId, List.of(questionId));
    }

    @Test
    void adjustQuestions_completedPlan_throwsException() {
        // Arrange
        LearningPlan plan = LearningPlan.builder()
            .id(planId)
            .childId(childId)
            .status("completed")
            .build();
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));

        UUID questionId = UUID.randomUUID();

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
            () -> planService.adjustQuestions(planId, List.of(questionId)));
        assertEquals("INVALID_STATUS", ex.getCode());
    }

    @Test
    void adjustQuestions_cancelledPlan_throwsException() {
        // Arrange
        LearningPlan plan = LearningPlan.builder()
            .id(planId)
            .childId(childId)
            .status("cancelled")
            .build();
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));

        UUID questionId = UUID.randomUUID();

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
            () -> planService.adjustQuestions(planId, List.of(questionId)));
        assertEquals("INVALID_STATUS", ex.getCode());
    }

    @Test
    void adjustQuestions_emptyList_noOperation() {
        // Arrange
        LearningPlan plan = LearningPlan.builder()
            .id(planId)
            .childId(childId)
            .status("active")
            .build();
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));

        // Act
        planService.adjustQuestions(planId, List.of());

        // Assert
        verify(planQuestionRepository, never()).deleteByPlanIdAndQuestionIdIn(any(), any());
    }
}
