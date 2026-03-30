package com.kidslearn.content.service;

import com.kidslearn.config.AppProperties;
import com.kidslearn.content.domain.LearningSession;
import com.kidslearn.content.domain.Question;
import com.kidslearn.content.dto.QuizNextRequest;
import com.kidslearn.content.dto.QuizNextResponse;
import com.kidslearn.content.dto.QuizSubmitRequest;
import com.kidslearn.content.repository.LearningSessionRepository;
import com.kidslearn.content.repository.QuestionRepository;
import com.kidslearn.content.repository.QuizAnswerRepository;
import com.kidslearn.report.domain.StudyRecord;
import com.kidslearn.report.repository.StudyRecordRepository;
import com.kidslearn.user.domain.Child;
import com.kidslearn.user.repository.ChildRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock private QuestionRepository questionRepository;
    @Mock private LearningSessionRepository sessionRepository;
    @Mock private QuizAnswerRepository answerRepository;
    @Mock private ChildRepository childRepository;
    @Mock private StudyRecordRepository studyRecordRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private AppProperties appProperties;

    @InjectMocks
    private QuizService quizService;

    private final UUID childId = UUID.randomUUID();
    private final UUID questionId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();

    private Child mockChild() {
        Child child = new Child();
        child.setId(childId);
        child.setStars(0);
        child.setXp(0);
        child.setDailyTargetEn(5);
        child.setDailyTargetMa(3);
        return child;
    }

    private Question mockQuestion() {
        Question q = new Question();
        q.setId(questionId);
        q.setSubject("en");
        q.setAuditStatus("approved");
        q.setScheduledDate(LocalDate.now());
        q.setLabel("Test Label");
        q.setQuestion("1+1=?");
        q.setCorrectAnswerId("A");
        q.setOptions(List.of(Map.of("id", "A", "text", "2"), Map.of("id", "B", "text", "3")));
        q.setEstimatedMinutes(3);
        return q;
    }

    @BeforeEach
    void setUp() {
        AppProperties.QuizProperties quizProps = new AppProperties.QuizProperties(0.2, 2, 20, 4, 20, 0, 16);
        lenient().when(appProperties.quiz()).thenReturn(quizProps);
    }

    // ===== getNextQuestion 测试 =====

    @Test
    void getNextQuestion_hasQuestion_returnsHasQuestion() {
        // Arrange
        Child child = mockChild();
        when(childRepository.findById(childId)).thenReturn(Optional.of(child));
        when(studyRecordRepository.findByChildIdAndRecordDate(childId, LocalDate.now())).thenReturn(Collections.emptyList());

        Question q = mockQuestion();
        when(questionRepository.findUnansweredByScheduledDate("en", LocalDate.now(), childId))
            .thenReturn(List.of(q));

        // Act
        var resp = quizService.getNextQuestion(new QuizNextRequest("en", null), childId);

        // Assert
        assertInstanceOf(QuizNextResponse.HasQuestion.class, resp);
        var hasQ = (QuizNextResponse.HasQuestion) resp;
        assertEquals(questionId, hasQ.question().id());
        assertEquals("en", hasQ.question().subject());

        // 验证 summary 字段
        assertTrue(hasQ.summary().hasMore());
        assertTrue(hasQ.summary().todayMaxStars() > 0);
        assertEquals(5, hasQ.summary().todayTargetEn());
        assertEquals(3, hasQ.summary().todayTargetMa());
    }

    @Test
    void getNextQuestion_noQuestions_returnsNoMoreQuestions() {
        // Arrange
        Child child = mockChild();
        when(childRepository.findById(childId)).thenReturn(Optional.of(child));
        when(studyRecordRepository.findByChildIdAndRecordDate(childId, LocalDate.now())).thenReturn(Collections.emptyList());
        when(questionRepository.findUnansweredByScheduledDate("en", LocalDate.now(), childId))
            .thenReturn(Collections.emptyList());

        // Act
        var resp = quizService.getNextQuestion(new QuizNextRequest("en", null), childId);

        // Assert
        assertInstanceOf(QuizNextResponse.NoMoreQuestions.class, resp);
        var noMore = (QuizNextResponse.NoMoreQuestions) resp;
        // hasMore 基于剩余目标而非是否有题；今日目标 en=5 ma=3，均未完成 → hasMore=true
        assertTrue(noMore.summary().hasMore());
    }

    @Test
    void getNextQuestion_noQuestionsButHasTargets_hasMoreIsTrue() {
        // Arrange: 无题可答，但今日目标尚未完成
        Child child = mockChild();
        when(childRepository.findById(childId)).thenReturn(Optional.of(child));
        when(studyRecordRepository.findByChildIdAndRecordDate(childId, LocalDate.now())).thenReturn(Collections.emptyList());
        when(questionRepository.findUnansweredByScheduledDate("en", LocalDate.now(), childId))
            .thenReturn(Collections.emptyList());

        // Act
        var resp = quizService.getNextQuestion(new QuizNextRequest("en", null), childId);

        // Assert
        assertInstanceOf(QuizNextResponse.NoMoreQuestions.class, resp);
        var noMore = (QuizNextResponse.NoMoreQuestions) resp;
        // todayMaxStars = (enRemaining + maRemaining) * baseStars = (5 + 3) * 2 = 16
        assertEquals(16, noMore.summary().todayMaxStars());
        // todayTotal = todayDone(0) + enRemaining(5) + maRemaining(3) = 8
        assertEquals(8, noMore.summary().todayTotal());
    }

    @Test
    void getNextQuestion_withAnsweredRecord_includesStatsInSummary() {
        // Arrange
        Child child = mockChild();
        when(childRepository.findById(childId)).thenReturn(Optional.of(child));

        StudyRecord enRecord = StudyRecord.builder()
            .childId(childId).recordDate(LocalDate.now()).subject("en")
            .questionCount(3).starsEarned(6).build();
        when(studyRecordRepository.findByChildIdAndRecordDate(childId, LocalDate.now()))
            .thenReturn(List.of(enRecord));
        when(questionRepository.findUnansweredByScheduledDate("en", LocalDate.now(), childId))
            .thenReturn(Collections.emptyList());

        // Act
        var resp = quizService.getNextQuestion(new QuizNextRequest("en", null), childId);

        // Assert
        assertInstanceOf(QuizNextResponse.NoMoreQuestions.class, resp);
        var noMore = (QuizNextResponse.NoMoreQuestions) resp;
        assertEquals(3, noMore.summary().todayDone());
        assertEquals(6, noMore.summary().todayStarsEarned());
    }

    // ===== submitAnswer 测试 =====

    @Test
    void submitAnswer_correct_includesTodayMaxStars() {
        // Arrange
        Child child = mockChild();
        when(childRepository.findById(childId)).thenReturn(Optional.of(child));

        Question q = mockQuestion();
        q.setCorrectAnswerId("A");
        q.setSuccessResultMode("correct");
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(q));

        LearningSession session = LearningSession.builder()
            .id(sessionId).childId(childId).currentCombo(0).longestCombo(0).build();
        when(sessionRepository.findTopByChildIdAndEndedAtIsNullOrderByStartedAtDesc(childId))
            .thenReturn(Optional.of(session));
        when(studyRecordRepository.findByChildIdAndRecordDateAndSubject(eq(childId), any(LocalDate.class), any()))
            .thenReturn(Optional.empty());
        when(studyRecordRepository.save(any(StudyRecord.class))).thenAnswer(i -> i.getArgument(0));

        QuizSubmitRequest req = new QuizSubmitRequest(questionId, null, "A", 15);

        // Act
        var response = quizService.submitAnswer(req, childId);

        // Assert
        assertTrue(response.isCorrect());
        assertEquals("correct", response.resultMode());
        assertEquals(1, response.result().combo());
        // todayMaxStars 应来自 buildQuizSummary，不应为 0
        assertTrue(response.result().todayMaxStars() >= 0);

        verify(answerRepository, times(1)).save(any());
        verify(childRepository, times(1)).addStars(childId, 2);
        verify(eventPublisher, times(1)).publishEvent(any(Object.class));
    }

    @Test
    void submitAnswer_wrong_resetsCombo() {
        // Arrange
        Child child = mockChild();
        when(childRepository.findById(childId)).thenReturn(Optional.of(child));

        Question q = mockQuestion();
        q.setCorrectAnswerId("A");
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(q));

        LearningSession session = LearningSession.builder()
            .id(sessionId).childId(childId).currentCombo(3).longestCombo(3).build();
        when(sessionRepository.findTopByChildIdAndEndedAtIsNullOrderByStartedAtDesc(childId))
            .thenReturn(Optional.of(session));
        when(studyRecordRepository.findByChildIdAndRecordDateAndSubject(eq(childId), any(LocalDate.class), any()))
            .thenReturn(Optional.empty());
        when(studyRecordRepository.save(any(StudyRecord.class))).thenAnswer(i -> i.getArgument(0));

        QuizSubmitRequest req = new QuizSubmitRequest(questionId, null, "B", 15);

        // Act
        var response = quizService.submitAnswer(req, childId);

        // Assert
        assertFalse(response.isCorrect());
        assertEquals("wrong", response.resultMode());
        assertEquals(0, response.result().combo());
        assertEquals(0, response.result().stars());

        verify(childRepository, never()).addStars(any(), anyInt());
        verify(childRepository, never()).addXp(any(), anyInt());
    }
}
