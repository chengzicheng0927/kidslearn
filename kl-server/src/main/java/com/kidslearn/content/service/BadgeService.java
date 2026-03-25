package com.kidslearn.content.service;

import com.kidslearn.common.event.AnswerSubmittedEvent;
import com.kidslearn.content.domain.Badge;
import com.kidslearn.content.domain.BadgeDefinition;
import com.kidslearn.content.dto.BadgeDTO;
import com.kidslearn.content.repository.BadgeDefinitionRepository;
import com.kidslearn.content.repository.BadgeRepository;
import com.kidslearn.user.repository.ChildRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BadgeService {

    private final BadgeRepository badgeRepository;
    private final BadgeDefinitionRepository badgeDefinitionRepository;
    private final ChildRepository childRepository;

    @Transactional
    public BadgeDTO getBadges(UUID childId) {
        // 获取所有徽章定义
        List<BadgeDefinition> all = badgeDefinitionRepository.findAll();

        // 获取孩子已获得的徽章
        List<Badge> earned = badgeRepository.findByChildId(childId);
        List<String> earnedIds = earned.stream()
                .map(Badge::getBadgeId)
                .toList();

        // 获取孩子的累计答题数（从 child 表）
        int totalAnswered = childRepository.findById(childId)
                .map(c -> c.getTotalAnswered())
                .orElse(0);

        // 计算当前进度等级
        int currentLevel = calculateCurrentLevel(totalAnswered, all);

        List<BadgeDTO.BadgeItem> badges = all.stream()
                .sorted((a, b) -> Integer.compare(a.getLevel(), b.getLevel()))
                .map(def -> new BadgeDTO.BadgeItem(
                        def.getId(),
                        def.getLevel(),
                        def.getIcon(),
                        def.getTitle(),
                        def.getDescription(),
                        def.getRequirement()
                ))
                .toList();

        return new BadgeDTO(badges, earnedIds, totalAnswered, currentLevel);
    }

    /**
     * 答题事件监听：判定是否解锁新徽章
     */
    @Transactional
    @EventListener
    public void onAnswerSubmitted(AnswerSubmittedEvent event) {
        if (!event.isCorrect()) return;

        // 获取当前等级和下一个徽章
        int totalAnswered = childRepository.findById(event.childId())
                .map(c -> c.getTotalAnswered())
                .orElse(0);

        List<BadgeDefinition> all = badgeDefinitionRepository.findAll();

        for (BadgeDefinition def : all) {
            if (totalAnswered >= def.getRequirement()) {
                String badgeId = def.getId();
                // 检查是否已获得
                if (!badgeRepository.existsByChildIdAndBadgeId(event.childId(), badgeId)) {
                    Badge badge = Badge.builder()
                            .childId(event.childId())
                            .badgeId(badgeId)
                            .earnedAt(Instant.now())
                            .build();
                    badgeRepository.save(badge);
                    log.info("BADGE_EARNED | childId={} | badgeId={} | title={}",
                            event.childId(), badgeId, def.getTitle());
                }
            }
        }
    }

    private int calculateCurrentLevel(int totalAnswered, List<BadgeDefinition> all) {
        int level = 0;
        for (BadgeDefinition def : all) {
            if (totalAnswered >= def.getRequirement()) {
                level = def.getLevel();
            }
        }
        return level;
    }
}
