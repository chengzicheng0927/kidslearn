package com.kidslearn.content.service;

import com.kidslearn.common.event.AnswerSubmittedEvent;
import com.kidslearn.content.domain.PlanQuestion;
import com.kidslearn.content.domain.WrongQuestion;
import com.kidslearn.content.repository.PlanQuestionRepository;
import com.kidslearn.content.repository.WrongQuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WrongQuestionServiceTest {

    @Mock
    private WrongQuestionRepository wrongQuestionRepository;

    @Mock
    private PlanQuestionRepository planQuestionRepository;

    @InjectMocks
    private WrongQuestionService wrongQuestionService;

    private UUID childId;
    private UUID questionId;

    @BeforeEach
    void setUp() {
        childId = UUID.randomUUID();
        questionId = UUID.randomUUID();
    }

    @Test
    void onAnswerSubmitted_correctAnswer_withoutPlan_doesNothing() {
        // given
        AnswerSubmittedEvent event = new AnswerSubmittedEvent(
            childId, UUID.randomUUID(), questionId, null, "a", true, 1, "en", "animals", Instant.now()
        );

        // when
        wrongQuestionService.onAnswerSubmitted(event);

        // then
        verify(wrongQuestionRepository, never()).save(any());
    }

    @Test
    void onAnswerSubmitted_wrongAnswer_savesNewWrongQuestion() {
        // given
        AnswerSubmittedEvent event = new AnswerSubmittedEvent(
            childId, UUID.randomUUID(), questionId, null, "b", false, 0, "en", "animals", Instant.now()
        );
        when(wrongQuestionRepository.findByChildIdAndQuestionId(childId, questionId)).thenReturn(Optional.empty());

        // when
        wrongQuestionService.onAnswerSubmitted(event);

        // then
        ArgumentCaptor<WrongQuestion> captor = ArgumentCaptor.forClass(WrongQuestion.class);
        verify(wrongQuestionRepository).save(captor.capture());
        WrongQuestion saved = captor.getValue();
        assertThat(saved.getChildId()).isEqualTo(childId);
        assertThat(saved.getQuestionId()).isEqualTo(questionId);
        assertThat(saved.getWrongAnswerId()).isEqualTo("b");
    }

    @Test
    void onAnswerSubmitted_correctAnswer_withPlan_incrementsCorrectCount() {
        // given
        UUID planQuestionId = UUID.randomUUID();
        UUID wrongQuestionId = UUID.randomUUID();
        AnswerSubmittedEvent event = new AnswerSubmittedEvent(
            childId, UUID.randomUUID(), questionId, planQuestionId, "a", true, 1, "en", "animals", Instant.now()
        );

        PlanQuestion pq = PlanQuestion.builder()
            .id(planQuestionId).wrongQuestionId(wrongQuestionId).build();
        WrongQuestion wq = WrongQuestion.builder()
            .id(wrongQuestionId).correctCount(1).threshold(3).build();

        when(planQuestionRepository.findById(planQuestionId)).thenReturn(Optional.of(pq));
        when(wrongQuestionRepository.findById(wrongQuestionId)).thenReturn(Optional.of(wq));

        // when
        wrongQuestionService.onAnswerSubmitted(event);

        // then
        verify(wrongQuestionRepository).save(wq);
        assertThat(wq.getCorrectCount()).isEqualTo(2);
    }
}
