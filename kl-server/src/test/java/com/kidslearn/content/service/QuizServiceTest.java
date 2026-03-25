package com.kidslearn.content.service;

import com.kidslearn.config.AppProperties;
import com.kidslearn.content.domain.LearningSession;
import com.kidslearn.content.domain.Question;
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

    @BeforeEach
    void setUp() {
        // Mock properties
        AppProperties.QuizProperties quizProps = new AppProperties.QuizProperties(0.2, 10, 20, 10, 20, 0, 50);
        when(appProperties.quiz()).thenReturn(quizProps);

        // Mock Child
        Child child = new Child();
        child.setId(childId);
        child.setStars(0);
        child.setXp(0);
        when(childRepository.findById(childId)).thenReturn(Optional.of(child));

        // Mock Session
        LearningSession session = LearningSession.builder()
            .id(sessionId)
            .childId(childId)
            .currentCombo(0)
            .longestCombo(0)
            .build();
        when(sessionRepository.findTopByChildIdAndEndedAtIsNullOrderByStartedAtDesc(childId))
            .thenReturn(Optional.of(session));

        // Mock StudyRecordRepository (upsertStudyRecord 场景)
        when(studyRecordRepository.findByChildIdAndRecordDateAndSubject(eq(childId), any(LocalDate.class), any()))
            .thenReturn(Optional.empty());
        when(studyRecordRepository.save(any(StudyRecord.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void testSubmitAnswer_Correct() {
        // Arrange
        Question q = new Question();
        q.setId(questionId);
        q.setCorrectAnswerId("A");
        q.setSubject("en");
        q.setSuccessResultMode("correct");
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(q));

        QuizSubmitRequest req = new QuizSubmitRequest(questionId, null, "A", 15);

        // Act
        var response = quizService.submitAnswer(req, childId);

        // Assert
        assertTrue(response.isCorrect());
        assertEquals("correct", response.resultMode());
        assertEquals(1, response.result().combo()); // Combo incremented
        assertEquals(10, response.result().stars()); // Base stars
        
        verify(answerRepository, times(1)).save(any());
        verify(sessionRepository, times(1)).save(any());
        verify(childRepository, times(1)).addStars(childId, 10);
        verify(childRepository, times(1)).addXp(childId, 20);
        verify(eventPublisher, times(1)).publishEvent(any(Object.class));
    }

    @Test
    void testSubmitAnswer_Wrong() {
        // Arrange
        Question q = new Question();
        q.setId(questionId);
        q.setCorrectAnswerId("A");
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(q));

        QuizSubmitRequest req = new QuizSubmitRequest(questionId, null, "B", 15);

        // Act
        var response = quizService.submitAnswer(req, childId);

        // Assert
        assertFalse(response.isCorrect());
        assertEquals("wrong", response.resultMode());
        assertEquals(0, response.result().combo()); // Combo reset

        // Rewards for wrong answer are 0
        assertEquals(0, response.result().stars()); 
        
        verify(childRepository, never()).addStars(any(), anyInt());
        verify(childRepository, never()).addXp(any(), anyInt());
    }
}
