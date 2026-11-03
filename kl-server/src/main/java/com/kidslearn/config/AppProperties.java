package com.kidslearn.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app")
@Validated
public record AppProperties(
    QuizProperties quiz,
    PlanProperties plan,
    BankGenerationProperties bankGeneration
) {

    /**
     * 答题与奖励配置
     */
    public record QuizProperties(
        double rewardQuestionRatio,
        int baseStars,
        int baseXp,
        int rewardStars,
        int rewardXp,
        int wrongDeductionStars,
        int dailyStarsLimit
    ) {
        public QuizProperties {
            if (rewardQuestionRatio <= 0 || rewardQuestionRatio > 1) rewardQuestionRatio = 0.2;
            if (baseStars <= 0) baseStars = 8;
            if (baseXp <= 0) baseXp = 18;
            if (rewardStars <= 0) rewardStars = 10;
            if (rewardXp <= 0) rewardXp = 20;
            if (dailyStarsLimit <= 0) dailyStarsLimit = 50;
        }
    }

    /**
     * 学习计划配置
     */
    public record PlanProperties(
        int defaultDurationDays,
        String defaultDifficulty
    ) {
        public PlanProperties {
            if (defaultDurationDays <= 0) defaultDurationDays = 14;
            if (defaultDifficulty == null) defaultDifficulty = "medium";
        }
    }

    /**
     * 题库预生成配置
     */
    public record BankGenerationProperties(
        String cron,
        int minWaterline,
        int maxWaterline,
        int defaultBatchCount
    ) {
        public BankGenerationProperties {
            if (cron == null) cron = "0 0 3 * * ?";
            if (minWaterline <= 0) minWaterline = 50;
            if (maxWaterline <= minWaterline) maxWaterline = minWaterline * 2;
            if (defaultBatchCount <= 0) defaultBatchCount = 50;
        }
    }
}
