package com.kidslearn.content.service;

import com.kidslearn.common.event.AnswerSubmittedEvent;
import com.kidslearn.content.domain.PlanQuestion;
import com.kidslearn.content.domain.WrongQuestion;
import com.kidslearn.content.repository.PlanQuestionRepository;
import com.kidslearn.content.repository.WrongQuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WrongQuestionService {

    private final WrongQuestionRepository wrongQuestionRepository;
    private final PlanQuestionRepository planQuestionRepository;

    /**
     * 监听答题事件，记录或处理条目进度
     */
    @EventListener
    @Transactional
    public void onAnswerSubmitted(AnswerSubmittedEvent event) {
        if (!event.isCorrect()) {
            handleWrongAnswer(event);
        } else {
            handleCorrectAnswer(event);
        }
    }

    private void handleWrongAnswer(AnswerSubmittedEvent event) {
        log.info("WRONG_QUESTION_EVENT | childId={} | questionId={} | selected={}",
            event.childId(), event.questionId(), event.selectedAnswer());

        WrongQuestion wq = wrongQuestionRepository.findByChildIdAndQuestionId(event.childId(), event.questionId())
            .orElseGet(() -> WrongQuestion.builder()
                .childId(event.childId())
                .questionId(event.questionId())
                .wrongCount(0)
                .correctCount(0)
                .threshold(3)
                .build());

        wq.setWrongAnswerId(event.selectedAnswer());
        wq.setWrongCount(wq.getWrongCount() + 1);
        wq.setCorrectCount(0); // 答错立即重置进度为0
        wq.setAnsweredAt(event.answeredAt());

        wrongQuestionRepository.save(wq);
    }

    private void handleCorrectAnswer(AnswerSubmittedEvent event) {
        if (event.planQuestionId() == null) {
            return;
        }

        // 如果是针对错题的巩固练习（带有 planQuestionId 且关联了 wrongQuestionId）
        planQuestionRepository.findById(event.planQuestionId()).ifPresent(pq -> {
            if (pq.getWrongQuestionId() != null) {
                wrongQuestionRepository.findById(pq.getWrongQuestionId()).ifPresent(wq -> {
                    // 如果还没达到阈值，则增加连续正确计数
                    if (wq.getCorrectCount() < wq.getThreshold()) {
                        wq.setCorrectCount(wq.getCorrectCount() + 1);
                        wrongQuestionRepository.save(wq);
                        log.info("CONSOLIDATION_SUCCESS | childId={} | questionId={} | correctCount={}/{}",
                            event.childId(), event.questionId(), wq.getCorrectCount(), wq.getThreshold());
                    }
                });
            }
        });
    }
}
