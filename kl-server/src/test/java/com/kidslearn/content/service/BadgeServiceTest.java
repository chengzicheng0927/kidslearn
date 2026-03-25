package com.kidslearn.content.service;

import com.kidslearn.common.event.AnswerSubmittedEvent;
import com.kidslearn.content.domain.Badge;
import com.kidslearn.content.domain.BadgeDefinition;
import com.kidslearn.content.dto.BadgeDTO;
import com.kidslearn.content.repository.BadgeDefinitionRepository;
import com.kidslearn.content.repository.BadgeRepository;
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
class BadgeServiceTest {

    @Mock
    private BadgeRepository badgeRepository;

    @Mock
    private BadgeDefinitionRepository badgeDefinitionRepository;

    @Mock
    private ChildRepository childRepository;

    @InjectMocks
    private BadgeService badgeService;

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
                .totalAnswered(25)
                .build();
    }

    private BadgeDefinition makeBadge(String id, int level, String icon, String title, String description, int requirement) {
        return BadgeDefinition.builder()
                .id(id).level(level).icon(icon).title(title)
                .description(description).requirement(requirement).build();
    }

    @Test
    void getBadges_returnsAllBadgeDefinitionsAndEarnedIds() {
        // given
        BadgeDefinition bronze = makeBadge("badge-bronze", 1, "bronze", "铜牌", "答对10题", 10);
        BadgeDefinition silver = makeBadge("badge-silver", 2, "silver", "银牌", "答对25题", 25);
        BadgeDefinition gold = makeBadge("badge-gold", 3, "gold", "金牌", "答对50题", 50);

        when(badgeDefinitionRepository.findAll()).thenReturn(List.of(bronze, silver, gold));

        Badge earnedBadge = Badge.builder()
                .id(UUID.randomUUID()).childId(childId).badgeId("badge-bronze").earnedAt(Instant.now()).build();
        when(badgeRepository.findByChildId(childId)).thenReturn(List.of(earnedBadge));
        when(childRepository.findById(childId)).thenReturn(Optional.of(child));

        // when
        BadgeDTO result = badgeService.getBadges(childId);

        // then
        assertThat(result.badges()).hasSize(3);
        assertThat(result.earnedIds()).containsExactly("badge-bronze");
        assertThat(result.totalAnswered()).isEqualTo(25);
        assertThat(result.currentLevel()).isEqualTo(2); // silver (25 >= 25 requirement)
    }

    @Test
    void getBadges_noBadgesEarned_returnsZeroLevel() {
        // given
        Child childNoBadges = Child.builder()
                .id(childId).parentId(UUID.randomUUID()).name("Child").age(6)
                .ageGroup("5-6岁").totalAnswered(5).build(); // less than bronze requirement of 10

        BadgeDefinition bronze = makeBadge("badge-bronze", 1, "bronze", "铜牌", "答对10题", 10);
        when(badgeDefinitionRepository.findAll()).thenReturn(List.of(bronze));
        when(badgeRepository.findByChildId(childId)).thenReturn(List.of());
        when(childRepository.findById(childId)).thenReturn(Optional.of(childNoBadges));

        // when
        BadgeDTO result = badgeService.getBadges(childId);

        // then
        assertThat(result.earnedIds()).isEmpty();
        assertThat(result.currentLevel()).isZero();
    }

    @Test
    void getBadges_noChild_returnsZeroStats() {
        // given
        when(badgeDefinitionRepository.findAll()).thenReturn(List.of());
        when(badgeRepository.findByChildId(childId)).thenReturn(List.of());
        when(childRepository.findById(childId)).thenReturn(Optional.empty());

        // when
        BadgeDTO result = badgeService.getBadges(childId);

        // then
        assertThat(result.totalAnswered()).isZero();
        assertThat(result.currentLevel()).isZero();
    }

    @Test
    void onAnswerSubmitted_correctAnswerUnlocksBadge() {
        // given
        Child childWith10 = Child.builder()
                .id(childId).parentId(UUID.randomUUID()).name("Child").age(6)
                .ageGroup("5-6岁").totalAnswered(10).build();
        when(childRepository.findById(childId)).thenReturn(Optional.of(childWith10));

        BadgeDefinition bronze = makeBadge("badge-bronze", 1, "bronze", "铜牌", "答对10题", 10);
        when(badgeDefinitionRepository.findAll()).thenReturn(List.of(bronze));
        when(badgeRepository.existsByChildIdAndBadgeId(childId, "badge-bronze")).thenReturn(false);

        AnswerSubmittedEvent event = new AnswerSubmittedEvent(
                childId, UUID.randomUUID(), UUID.randomUUID(),
                true, 1, "en", "animals", Instant.now()
        );

        // when
        badgeService.onAnswerSubmitted(event);

        // then
        ArgumentCaptor<Badge> captor = ArgumentCaptor.forClass(Badge.class);
        verify(badgeRepository).save(captor.capture());
        Badge saved = captor.getValue();
        assertThat(saved.getChildId()).isEqualTo(childId);
        assertThat(saved.getBadgeId()).isEqualTo("badge-bronze");
    }

    @Test
    void onAnswerSubmitted_wrongAnswer_doesNotUnlockBadge() {
        // given - no stubs needed since method returns early when isCorrect is false

        AnswerSubmittedEvent event = new AnswerSubmittedEvent(
                childId, UUID.randomUUID(), UUID.randomUUID(),
                false, 0, "en", "animals", Instant.now()
        );

        // when
        badgeService.onAnswerSubmitted(event);

        // then
        verify(badgeRepository, never()).save(any());
    }

    @Test
    void onAnswerSubmitted_alreadyEarned_doesNotSaveDuplicate() {
        // given
        Child childWith10 = Child.builder()
                .id(childId).parentId(UUID.randomUUID()).name("Child").age(6)
                .ageGroup("5-6岁").totalAnswered(10).build();
        when(childRepository.findById(childId)).thenReturn(Optional.of(childWith10));

        BadgeDefinition bronze = makeBadge("badge-bronze", 1, "bronze", "铜牌", "答对10题", 10);
        when(badgeDefinitionRepository.findAll()).thenReturn(List.of(bronze));
        when(badgeRepository.existsByChildIdAndBadgeId(childId, "badge-bronze")).thenReturn(true);

        AnswerSubmittedEvent event = new AnswerSubmittedEvent(
                childId, UUID.randomUUID(), UUID.randomUUID(),
                true, 1, "en", "animals", Instant.now()
        );

        // when
        badgeService.onAnswerSubmitted(event);

        // then
        verify(badgeRepository, never()).save(any());
    }

    @Test
    void onAnswerSubmitted_multipleBadgesUnlocked_savesAll() {
        // given
        Child childWith100 = Child.builder()
                .id(childId).parentId(UUID.randomUUID()).name("Child").age(6)
                .ageGroup("5-6岁").totalAnswered(100).build();
        when(childRepository.findById(childId)).thenReturn(Optional.of(childWith100));

        BadgeDefinition bronze = makeBadge("badge-bronze", 1, "bronze", "铜牌", "答对10题", 10);
        BadgeDefinition silver = makeBadge("badge-silver", 2, "silver", "银牌", "答对25题", 25);
        BadgeDefinition gold = makeBadge("badge-gold", 3, "gold", "金牌", "答对50题", 50);
        BadgeDefinition platinum = makeBadge("badge-platinum", 4, "platinum", "铂金", "答对100题", 100);

        when(badgeDefinitionRepository.findAll()).thenReturn(List.of(bronze, silver, gold, platinum));
        when(badgeRepository.existsByChildIdAndBadgeId(any(), any())).thenReturn(false);

        AnswerSubmittedEvent event = new AnswerSubmittedEvent(
                childId, UUID.randomUUID(), UUID.randomUUID(),
                true, 0, "en", "animals", Instant.now()
        );

        // when
        badgeService.onAnswerSubmitted(event);

        // then
        verify(badgeRepository, times(4)).save(any());
    }
}
