package com.kidslearn.content.dto;

/**
 * 今日答题总结，由 getNextQuestion 在无题或答完题后返回。
 */
public record QuizSummaryDTO(
    /** 今日是否还有更多题目 */
    boolean hasMore,
    /** 今日总题目数（计划目标） */
    int todayTotal,
    /** 今日最高可获得星星数（计划目标） */
    int todayMaxStars,
    /** 今日已答题数 */
    int todayDone,
    /** 今日已获得星星 */
    int todayStarsEarned,
    /** 英语今日目标题数 */
    int todayTargetEn,
    /** 数学今日目标题数 */
    int todayTargetMa
) {}
