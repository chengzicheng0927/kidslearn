package com.kidslearn.content.service;

import com.kidslearn.common.event.AnswerSubmittedEvent;
import com.kidslearn.content.domain.Achievement;
import com.kidslearn.content.domain.AchievementDefinition;
import com.kidslearn.content.dto.AchievementDTO;
import com.kidslearn.content.dto.AchievementDTO.AchievementStats;
import com.kidslearn.content.repository.AchievementDefinitionRepository;
import com.kidslearn.content.repository.AchievementRepository;
import com.kidslearn.user.repository.ChildRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AchievementService {

    private final AchievementRepository achievementRepository;
    private final AchievementDefinitionRepository achievementDefinitionRepository;
    private final ChildRepository childRepository;

    public AchievementDTO getAchievements(UUID childId) {
        // 获取所有成就定义
        List<AchievementDefinition> all = achievementDefinitionRepository.findAll();

        // 获取已获得的成就
        List<Achievement> earned = achievementRepository.findByChildId(childId);
        List<String> earnedIds = earned.stream()
                .map(Achievement::getDefinitionId)
                .toList();

        // 获取统计数据
        AchievementStats stats = calculateStats(childId);

        List<AchievementDTO.AchievementItem> achievements = all.stream()
                .map(def -> new AchievementDTO.AchievementItem(
                        def.getId(),
                        def.getCategory(),
                        def.getIcon(),
                        def.getTitle(),
                        def.getDescription(),
                        def.getRequirement(),
                        def.getType()
                ))
                .toList();

        return new AchievementDTO(achievements, earnedIds, stats);
    }

    /**
     * 答题事件监听：判定成就解锁
     */
    @Transactional
    @EventListener
    public void onAnswerSubmitted(AnswerSubmittedEvent event) {
        UUID childId = event.childId();
        boolean isCorrect = event.isCorrect();

        // 1. 首发命中（第一题）
        checkAndAward(childId, "ach-special-1");

        // 2. 连击成就
        int combo = event.comboAtAnswer();
        if (isCorrect && combo >= 3) {
            if (combo >= 10) {
                checkAndAward(childId, "ach-streak-3"); // 连胜达人 10 题
                checkAndAward(childId, "ach-streak-2"); // 小宇宙爆发 5 题
            } else if (combo >= 5) {
                checkAndAward(childId, "ach-streak-2"); // 小宇宙爆发 5 题
            }
            checkAndAward(childId, "ach-streak-1"); // 初露锋芒 3 题
        }

        // 3. 题量成就
        int total = childRepository.findById(childId)
                .map(c -> c.getTotalAnswered())
                .orElse(0);

        if (total >= 1) checkAndAward(childId, "ach-special-1");
        if (total >= 10) checkAndAward(childId, "ach-vol-1");
        if (total >= 50) checkAndAward(childId, "ach-vol-2");
        if (total >= 100) checkAndAward(childId, "ach-vol-3");

        // 4. 连续学习天数成就
        int streakDays = childRepository.findById(childId)
                .map(c -> c.getStreak())
                .orElse(0);
        if (streakDays >= 7) checkAndAward(childId, "ach-special-2");

        log.debug("ACHIEVEMENT_CHECK | childId={} | correct={} | combo={} | total={}",
                childId, isCorrect, combo, total);
    }

    private void checkAndAward(UUID childId, String definitionId) {
        if (!achievementRepository.existsByChildIdAndDefinitionId(childId, definitionId)) {
            AchievementDefinition def = achievementDefinitionRepository.findById(definitionId).orElse(null);
            if (def == null) return;

            Achievement achievement = Achievement.builder()
                    .childId(childId)
                    .definitionId(definitionId)
                    .icon(def.getIcon())
                    .title(def.getTitle())
                    .description(def.getDescription())
                    .progressPct(100)
                    .earnedAt(Instant.now())
                    .build();
            achievementRepository.save(achievement);
            log.info("ACHIEVEMENT_EARNED | childId={} | defId={} | title={}",
                    childId, definitionId, def.getTitle());
        }
    }

    private AchievementStats calculateStats(UUID childId) {
        return childRepository.findById(childId)
                .map(child -> new AchievementStats(
                        child.getTotalAnswered(),
                        child.getStreak(),
                        0, // TODO: bestStreak 需要 session 表聚合
                        0,  // TODO: todayCorrect 需要 study_records 或 session 聚合
                        0   // TODO: todayTotal 同上
                ))
                .orElse(new AchievementStats(0, 0, 0, 0, 0));
    }
}
