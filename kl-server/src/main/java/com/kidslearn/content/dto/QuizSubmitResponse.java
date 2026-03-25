package com.kidslearn.content.dto;

import java.util.List;

public record QuizSubmitResponse(
    boolean isCorrect, String resultMode, String correctAnswerId,
    String explanation, List<String> explanationSteps, String encouragement,
    boolean showResult, boolean hasMore, ResultDTO result
) {
    public record ResultDTO(
        int combo, int accuracy, int timeSpent, int stars, int todayMaxStars,
        int xp, int xpNeeded, int level, BadgeDTO reward
    ) {
        public record BadgeDTO(String icon, String title, String sub) {}
    }
}
