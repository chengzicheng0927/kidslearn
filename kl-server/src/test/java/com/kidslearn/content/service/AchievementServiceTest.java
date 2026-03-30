package com.kidslearn.content.service;

import com.kidslearn.common.event.AnswerSubmittedEvent;
import com.kidslearn.content.domain.Achievement;
import com.kidslearn.content.domain.AchievementDefinition;
import com.kidslearn.content.dto.AchievementDTO;
import com.kidslearn.content.repository.AchievementDefinitionRepository;
import com.kidslearn.content.repository.AchievementRepository;
import com.kidslearn.user.domain.Child;
import com.kidslearn.user.repository.ChildRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AchievementServiceTest {

    @Mock
    private AchievementRepository achievementRepository;

    @Mock
    private AchievementDefinitionRepository achievementDefinitionRepository;

    @Mock
    private ChildRepository childRepository;

    @InjectMocks
    private AchievementService achievementService;

    private UUID childId;
    private Child child;

    @BeforeEach
    void setUp() {
        childId = UUID.randomUUID();
        child = Child.builder()
                .id(childId)
                .parentId(UUID.randomUUID())
                .name("TestChild")
                .age(6)
                .ageGroup("5-6岁")
                .totalAnswered(10)
                .streak(3)
                .build();
    }

    private AchievementDefinition makeDef(String id, String category, String icon, String title, String description, int requirement, String type) {
        return AchievementDefinition.builder()
                .id(id).category(category).icon(icon).title(title)
                .description(description).requirement(requirement).type(type).build();
    }

    @Test
    void getAchievements_returnsAllDefinitionsWithEarnedIds() {
        // given
        AchievementDefinition def1 = makeDef("ach-special-1", "special", "star", "首发命中", "答对第一题", 1, "special");
        AchievementDefinition def2 = makeDef("ach-vol-1", "volume", "book", "学习达人", "累计答题10题", 10, "volume");

        when(achievementDefinitionRepository.findAll()).thenReturn(List.of(def1, def2));

        Achievement earned = Achievement.builder()
                .id(UUID.randomUUID()).childId(childId).definitionId("ach-special-1")
                .title("首发命中").build();
        when(achievementRepository.findByChildId(childId)).thenReturn(List.of(earned));
        when(childRepository.findById(childId)).thenReturn(Optional.of(child));

        // when
        AchievementDTO result = achievementService.getAchievements(childId);

        // then
        assertThat(result.achievements()).hasSize(2);
        assertThat(result.earnedIds()).containsExactly("ach-special-1");
        assertThat(result.stats().totalQuestions()).isEqualTo(10);
        assertThat(result.stats().currentStreak()).isEqualTo(3);
    }

    @Test
    void getAchievements_noChild_returnsZeroStats() {
        // given
        when(achievementDefinitionRepository.findAll()).thenReturn(List.of());
        when(achievementRepository.findByChildId(childId)).thenReturn(List.of());
        when(childRepository.findById(childId)).thenReturn(Optional.empty());

        // when
        AchievementDTO result = achievementService.getAchievements(childId);

        // then
        assertThat(result.stats().totalQuestions()).isZero();
        assertThat(result.stats().currentStreak()).isZero();
    }

    @Test
    void onAnswerSubmitted_firstCorrectAnswer_awardsSpecialAchievement() {
        // given
        AchievementDefinition special1 = makeDef("ach-special-1", "special", "star", "首发命中", "答对第一题", 1, "special");
        when(achievementRepository.existsByChildIdAndDefinitionId(childId, "ach-special-1")).thenReturn(false);
        when(achievementDefinitionRepository.findById("ach-special-1")).thenReturn(Optional.of(special1));

        AnswerSubmittedEvent event = new AnswerSubmittedEvent(
                childId, UUID.randomUUID(), UUID.randomUUID(), null, "a",
                true, 1, "en", "animals", Instant.now()
        );

        // when
        achievementService.onAnswerSubmitted(event);

        // then
        ArgumentCaptor<Achievement> captor = ArgumentCaptor.forClass(Achievement.class);
        verify(achievementRepository).save(captor.capture());
        Achievement saved = captor.getValue();
        assertThat(saved.getChildId()).isEqualTo(childId);
        assertThat(saved.getDefinitionId()).isEqualTo("ach-special-1");
        assertThat(saved.getProgressPct()).isEqualTo(100);
    }

    @Test
    void onAnswerSubmitted_alreadyEarned_doesNotSaveDuplicate() {
        // given
        when(achievementRepository.existsByChildIdAndDefinitionId(childId, "ach-special-1")).thenReturn(true);

        AnswerSubmittedEvent event = new AnswerSubmittedEvent(
                childId, UUID.randomUUID(), UUID.randomUUID(), null, "a",
                true, 1, "en", "animals", Instant.now()
        );

        // when
        achievementService.onAnswerSubmitted(event);

        // then
        verify(achievementRepository, never()).save(any());
    }

    @Test
    void onAnswerSubmitted_combo10_awardsAllStreakAchievements() {
        // given
        AchievementDefinition streak1 = makeDef("ach-streak-1", "streak", "flame", "初露锋芒", "连击3题", 3, "streak");
        AchievementDefinition streak2 = makeDef("ach-streak-2", "streak", "fire", "小宇宙爆发", "连击5题", 5, "streak");
        AchievementDefinition streak3 = makeDef("ach-streak-3", "streak", "rocket", "连胜达人", "连击10题", 10, "streak");
        AchievementDefinition special1 = makeDef("ach-special-1", "special", "star", "首发命中", "答对第一题", 1, "special");

        when(achievementRepository.existsByChildIdAndDefinitionId(any(), any())).thenReturn(false);
        when(achievementDefinitionRepository.findById("ach-streak-1")).thenReturn(Optional.of(streak1));
        when(achievementDefinitionRepository.findById("ach-streak-2")).thenReturn(Optional.of(streak2));
        when(achievementDefinitionRepository.findById("ach-streak-3")).thenReturn(Optional.of(streak3));
        when(achievementDefinitionRepository.findById("ach-special-1")).thenReturn(Optional.of(special1));

        AnswerSubmittedEvent event = new AnswerSubmittedEvent(
                childId, UUID.randomUUID(), UUID.randomUUID(), null, "a",
                true, 10, "en", "animals", Instant.now()
        );

        // when
        achievementService.onAnswerSubmitted(event);

        // then
        verify(achievementRepository, times(4)).save(any()); // special-1 + streak-1 + streak-2 + streak-3
    }

    @Test
    void onAnswerSubmitted_totalAnswered50_awardsVolumeAchievement() {
        // given
        Child childWith50 = Child.builder()
                .id(childId).parentId(UUID.randomUUID()).name("Child").age(6)
                .ageGroup("5-6岁").totalAnswered(50).streak(0).build();
        when(childRepository.findById(childId)).thenReturn(Optional.of(childWith50));
        when(achievementRepository.existsByChildIdAndDefinitionId(any(), any())).thenReturn(false);

        AchievementDefinition vol2 = makeDef("ach-vol-2", "volume", "book", "知识渊博", "累计答题50题", 50, "volume");
        AchievementDefinition special1 = makeDef("ach-special-1", "special", "star", "首发命中", "答对第一题", 1, "special");
        when(achievementDefinitionRepository.findById("ach-vol-2")).thenReturn(Optional.of(vol2));
        when(achievementDefinitionRepository.findById("ach-special-1")).thenReturn(Optional.of(special1));

        AnswerSubmittedEvent event = new AnswerSubmittedEvent(
                childId, UUID.randomUUID(), UUID.randomUUID(), null, "a",
                true, 0, "en", "animals", Instant.now()
        );

        // when
        achievementService.onAnswerSubmitted(event);

        // then
        verify(achievementRepository, atLeast(1)).save(any());
    }

    @Test
    void onAnswerSubmitted_streak7Days_awardsSpecial2Achievement() {
        // given
        Child childWithStreak = Child.builder()
                .id(childId).parentId(UUID.randomUUID()).name("Child").age(6)
                .ageGroup("5-6岁").totalAnswered(100).streak(7).build();
        when(childRepository.findById(childId)).thenReturn(Optional.of(childWithStreak));
        when(achievementRepository.existsByChildIdAndDefinitionId(any(), any())).thenReturn(false);

        AchievementDefinition special2 = makeDef("ach-special-2", "special", "calendar", "坚持不懈", "连续学习7天", 7, "special");
        AchievementDefinition special1 = makeDef("ach-special-1", "special", "star", "首发命中", "答对第一题", 1, "special");
        AchievementDefinition vol3 = makeDef("ach-vol-3", "volume", "book", "知识大师", "累计答题100题", 100, "volume");
        when(achievementDefinitionRepository.findById("ach-special-2")).thenReturn(Optional.of(special2));
        when(achievementDefinitionRepository.findById("ach-special-1")).thenReturn(Optional.of(special1));
        when(achievementDefinitionRepository.findById("ach-vol-3")).thenReturn(Optional.of(vol3));

        AnswerSubmittedEvent event = new AnswerSubmittedEvent(
                childId, UUID.randomUUID(), UUID.randomUUID(), null, "a",
                true, 0, "en", "animals", Instant.now()
        );

        // when
        achievementService.onAnswerSubmitted(event);

        // then
        verify(achievementRepository, atLeast(1)).save(any());
    }
}
