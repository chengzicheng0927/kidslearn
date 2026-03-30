package com.kidslearn.content.dto;

/**
 * 获取下一题的响应。
 * 无论有无题目，都包含今日总结（用于前端展示进度和判断是否休息）。
 */
public sealed interface QuizNextResponse
    permits QuizNextResponse.HasQuestion,
            QuizNextResponse.NoMoreQuestions {

    QuizSummaryDTO summary();

    /** 有题目时返回 */
    record HasQuestion(QuestionDTO question, QuizSummaryDTO summary) implements QuizNextResponse {}

    /** 无更多题目时返回 */
    record NoMoreQuestions(QuizSummaryDTO summary) implements QuizNextResponse {}
}
