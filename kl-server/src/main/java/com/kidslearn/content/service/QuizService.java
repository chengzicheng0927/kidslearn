package com.kidslearn.content.service;

import com.kidslearn.common.event.AnswerSubmittedEvent;
import com.kidslearn.common.exception.ResourceNotFoundException;
import com.kidslearn.config.AppProperties;
import com.kidslearn.content.domain.*;
import com.kidslearn.content.dto.*;
import com.kidslearn.content.repository.*;
import com.kidslearn.report.domain.StudyRecord;
import com.kidslearn.report.repository.StudyRecordRepository;
import com.kidslearn.user.domain.Child;
import com.kidslearn.user.repository.ChildRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizService {

    private final QuestionRepository questionRepository;
    private final LearningSessionRepository sessionRepository;
    private final QuizAnswerRepository answerRepository;
    private final ChildRepository childRepository;
    private final StudyRecordRepository studyRecordRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AppProperties appProperties;

    /**
     * 获取下一道题。
     * - 只取 scheduledDate = today 的题目
     * - 无论有无题目，都返回 QuizNextResponse（包含今日总结供前端展示）
     */
    @Transactional(readOnly = true)
    public QuizNextResponse getNextQuestion(QuizNextRequest req, UUID childId) {
        String subject = req.subject() != null ? req.subject() : "en";
        LocalDate today = LocalDate.now();

        // 查询今日未答的题目（按计划日期分配）
        List<Question> questions = questionRepository.findUnansweredByScheduledDate(
            subject, today, childId
        );

        // 构建今日总结
        QuizSummaryDTO summary = buildQuizSummary(childId, today);

        if (questions.isEmpty()) {
            return new QuizNextResponse.NoMoreQuestions(summary);
        }

        Question q = questions.getFirst();
        return new QuizNextResponse.HasQuestion(toQuestionDTO(q), summary);
    }

    /**
     * 构建今日答题总结。
     * - todayTotal = 今日安排的题目总数
     * - todayDone = 今日已答题目数
     * - todayMaxStars = 今日全部题目满分（逐题累加星星数）
     * - todayStarsEarned = 今日已获得星星
     * - hasMore = 是否还有未答的题
     *
     * TODO: 性能优化 - 预计算方案
     * 当前实现每次调用都会查询 Question 表计算今日题目数和满分星星，调用频繁时性能开销较大。
     * 建议改为每日凌晨4点（或其他低峰时段）预计算并缓存到 Redis：
     *   - 每日凌晨4点定时任务计算"明日题目数"和"明日满分星星"
     *   - 存入 Redis，key 如 "daily_quiz:summary:{childId}:{date}"
     *   - getNextQuestion 时直接从缓存读取，仅在缓存未命中时回源查询
     * 这样可大幅减少数据库查询，提升响应速度。
     */
    private QuizSummaryDTO buildQuizSummary(UUID childId, LocalDate today) {
        var quizConfig = appProperties.quiz();

        // 今日所有安排的题目
        List<Question> todayQuestions = questionRepository.findAllByScheduledDate(today);
        int todayTotal = todayQuestions.size();

        // 计算今日全部题目的满分星星（逐题累加）
        int todayMaxStars = todayQuestions.stream().mapToInt(q -> {
            if ("reward".equals(q.getSuccessResultMode())) {
                return quizConfig.rewardStars();
            }
            return quizConfig.baseStars();
        }).sum();

        // 今日各科已答情况（从 StudyRecord 汇总）
        List<StudyRecord> todayRecords = studyRecordRepository.findByChildIdAndRecordDate(childId, today);
        int todayDone = todayRecords.stream().mapToInt(StudyRecord::getQuestionCount).sum();
        int todayStarsEarned = todayRecords.stream().mapToInt(StudyRecord::getStarsEarned).sum();

        boolean hasMore = todayDone < todayTotal;

        Child child = childRepository.findById(childId).orElseThrow();
        return new QuizSummaryDTO(hasMore, todayTotal, todayMaxStars, todayDone, todayStarsEarned,
                child.getDailyTargetEn(), child.getDailyTargetMa());
    }

    /**
     * 提交答案
     */
    @Transactional
    public QuizSubmitResponse submitAnswer(QuizSubmitRequest req, UUID childId) {
        // 1. 查询题目
        Question q = questionRepository.findById(req.questionId())
            .orElseThrow(() -> new ResourceNotFoundException("Question", req.questionId()));

        // 2. 判定对错
        boolean isCorrect = q.getCorrectAnswerId().equals(req.answerId());

        // 3. 获取或创建会话
        LearningSession session = getOrCreateSession(childId, null);

        // 4. 更新 combo
        if (isCorrect) {
            session.setCurrentCombo(session.getCurrentCombo() + 1);
            session.setLongestCombo(Math.max(session.getLongestCombo(), session.getCurrentCombo()));
        } else {
            session.setCurrentCombo(0);
        }

        // 5. 确定 resultMode
        String resultMode = isCorrect
            ? (q.getSuccessResultMode() != null ? q.getSuccessResultMode() : "correct")
            : "wrong";

        // 6. 计算奖励
        var quizConfig = appProperties.quiz();
        int stars = 0, xp = 0;
        QuizSubmitResponse.ResultDTO.BadgeDTO badge = null;

        if (isCorrect) {
            if ("reward".equals(resultMode)) {
                stars = quizConfig.rewardStars();
                xp = quizConfig.rewardXp();
                badge = new QuizSubmitResponse.ResultDTO.BadgeDTO("🏅", "连击奖励", "太棒了！");
            } else {
                stars = quizConfig.baseStars();
                xp = quizConfig.baseXp();
            }
        }

        // 7. 写入答题记录
        QuizAnswer answer = QuizAnswer.builder()
            .sessionId(session.getId())
            .questionId(q.getId())
            .planQuestionId(req.planQuestionId())
            .selectedAnswer(req.answerId())
            .isCorrect(isCorrect)
            .timeSpentSec(req.timeSpent())
            .comboAtAnswer(session.getCurrentCombo())
            .snapshotTopic(q.getTopic())
            .snapshotStageKey(q.getTopicKey())
            .answeredAt(Instant.now())
            .build();
        answerRepository.save(answer);

        // 8. 更新会话统计
        session.setTotalQuestions(session.getTotalQuestions() + 1);
        if (isCorrect) session.setCorrectCount(session.getCorrectCount() + 1);
        session.setXpEarned(session.getXpEarned() + xp);
        session.setStarsEarned(session.getStarsEarned() + stars);
        sessionRepository.save(session);

        // 9. 更新孩子累计数据
        if (stars > 0) childRepository.addStars(childId, stars);
        if (xp > 0) childRepository.addXp(childId, xp);
        childRepository.incrementTotalAnswered(childId);

        // 10. 更新每日学习统计（按学科聚合，用于日历热力图）
        upsertStudyRecord(childId, q.getSubject(), q.getTopic(), stars, req.timeSpent());

        // 11. 发布领域事件
        eventPublisher.publishEvent(new AnswerSubmittedEvent(
            childId, session.getId(), q.getId(), req.planQuestionId(), req.answerId(), isCorrect,
            session.getCurrentCombo(), q.getSubject(), q.getTopicKey(), Instant.now()
        ));

        // 11. 计算正确率
        int accuracy = session.getTotalQuestions() > 0
            ? (int) (session.getCorrectCount() * 100.0 / session.getTotalQuestions())
            : 0;

        Child child = childRepository.findById(childId).orElseThrow();

        boolean showResult = !isCorrect || "reward".equals(resultMode);

        log.info("QUIZ_SUBMIT | childId={} | questionId={} | correct={} | combo={} | resultMode={}",
            childId, req.questionId(), isCorrect, session.getCurrentCombo(), resultMode);

        // 获取今日总结（包含真实 todayMaxStars）
        LocalDate today = LocalDate.now();
        QuizSummaryDTO summary = buildQuizSummary(childId, today);

        return new QuizSubmitResponse(
            isCorrect, resultMode, q.getCorrectAnswerId(),
            q.getExplanation(),
            q.getExplanationSteps() != null ? List.of(q.getExplanationSteps()) : List.of(),
            q.getEncouragement(),
            showResult, true,
            new QuizSubmitResponse.ResultDTO(
                session.getCurrentCombo(), accuracy, req.timeSpent(),
                stars, summary.todayMaxStars(),
                xp, child.getXpNeeded(), child.getLevel(), badge
            )
        );
    }

    /**
     * 更新每日学习统计（按 child + date + subject 聚合）
     * 对应 study_records 表，accuracy 由数据库 GENERATED ALWAYS AS 自动计算
     */
    private void upsertStudyRecord(UUID childId, String subject, String topic, int starsEarned, int timeSpentSec) {
        LocalDate today = LocalDate.now();
        int minutes = (timeSpentSec + 59) / 60; // 秒转分钟，向上取整

        StudyRecord record = studyRecordRepository
                .findByChildIdAndRecordDateAndSubject(childId, today, subject)
                .orElseGet(() -> StudyRecord.builder()
                        .childId(childId)
                        .recordDate(today)
                        .subject(subject)
                        .topic(topic)
                        .questionCount(0)
                        .correctCount(0)
                        .starsEarned(0)
                        .minutes(0)
                        .build());

        record.setQuestionCount(record.getQuestionCount() + 1);
        record.setStarsEarned(record.getStarsEarned() + starsEarned);
        record.setMinutes(record.getMinutes() + minutes);
        // accuracy 由数据库 GENERATED ALWAYS AS 自动计算，无需手动更新
        // topic 取最新题目的主题
        record.setTopic(topic);

        studyRecordRepository.save(record);
    }

    private LearningSession getOrCreateSession(UUID childId, UUID planId) {
        return sessionRepository.findTopByChildIdAndEndedAtIsNullOrderByStartedAtDesc(childId)
            .orElseGet(() -> {
                LearningSession s = LearningSession.builder()
                    .childId(childId)
                    .planId(planId)
                    .build();
                return sessionRepository.save(s);
            });
    }

    @SuppressWarnings("unchecked")
    private QuestionDTO toQuestionDTO(Question q) {
        List<QuestionDTO.QuizOption> options = q.getOptions().stream()
            .map(opt -> new QuestionDTO.QuizOption(
                opt.get("id"), opt.get("text"), opt.getOrDefault("emoji", "")
            ))
            .toList();

        return new QuestionDTO(
            q.getId(), null, null, q.getSubject(),
            q.getLabel(), q.getQuestion(), options, q.getEstimatedMinutes()
        );
    }
}
