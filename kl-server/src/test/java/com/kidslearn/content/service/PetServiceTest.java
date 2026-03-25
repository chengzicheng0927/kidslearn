package com.kidslearn.content.service;

import com.kidslearn.user.domain.Child;
import com.kidslearn.user.repository.ChildRepository;
import com.kidslearn.content.domain.PetState;
import com.kidslearn.common.exception.ResourceNotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PetServiceTest {

    @Mock private EntityManager em;
    @Mock private ChildRepository childRepository;
    @Mock private TypedQuery<PetState> query;

    @InjectMocks
    private PetService petService;

    private final UUID childId = UUID.randomUUID();
    private Child child;
    private PetState pet;

    @BeforeEach
    void setUp() {
        child = new Child();
        child.setId(childId);
        child.setStars(50); // 充沛的余额

        pet = new PetState();
        pet.setChildId(childId);
        pet.setPetName("Dino");
        pet.setEvolutionId(1);
        pet.setHunger(50);
        pet.setHappiness(50);
        pet.setXp(0);
        pet.setXpToNext(100);
    }

    private void mockGetPetState(PetState state) {
        when(em.createQuery(anyString(), eq(PetState.class))).thenReturn(query);
        when(query.setParameter("childId", childId)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(state));
    }

    @Test
    void testGetPetState_Success() {
        mockGetPetState(pet);
        PetState result = petService.getPetState(childId);
        assertNotNull(result);
        assertEquals("Dino", result.getPetName());
    }

    @Test
    void testFeedPet_Success() {
        mockGetPetState(pet);
        when(childRepository.findById(childId)).thenReturn(Optional.of(child));
        when(em.merge(any(PetState.class))).thenAnswer(i -> i.getArgument(0));

        PetState result = petService.feedPet(childId, "apple"); // cost 5

        assertEquals(45, child.getStars()); // 扣除5个星星
        assertEquals(70, result.getHunger()); // 饱食度上升
        assertEquals(55, result.getHappiness()); // 心情上升
        assertEquals(10, result.getXp()); // 增长经验
        verify(childRepository).save(child); // 保存扣费记录
        verify(em).merge(pet); // 保存宠物记录
    }

    @Test
    void testFeedPet_InsufficientStars() {
        mockGetPetState(pet);
        child.setStars(2); // 余额仅剩下2个星星，但食物花费5
        when(childRepository.findById(childId)).thenReturn(Optional.of(child));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            petService.feedPet(childId, "apple");
        });
        assertEquals("星星不足，快去答题赚取吧！", ex.getMessage());
        verify(em, never()).merge(any()); // 不应保存宠物记录
    }

    @Test
    void testInteractPet_TriggersEvolution() {
        pet.setXp(99); // Will exceed 100 after 2 xp gain
        mockGetPetState(pet);
        when(em.merge(any(PetState.class))).thenAnswer(i -> i.getArgument(0));

        PetState result = petService.interactPet(childId);

        assertEquals(0, result.getXp()); // Reset XP
        assertEquals(2, result.getEvolutionId()); // Evolved
        assertEquals(150, result.getXpToNext()); // 100 * 1.5
    }

    @Test
    void testGetPetState_NotFound() {
        when(em.createQuery(anyString(), eq(PetState.class))).thenReturn(query);
        when(query.setParameter("childId", childId)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of()); // Empty list returns null

        PetState result = petService.getPetState(childId);
        assertNull(result);
    }

    @Test
    void testFeedPet_ChildNotFound() {
        when(childRepository.findById(childId)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> {
            petService.feedPet(childId, "apple"); // cost 5
        });
        assertTrue(ex.getMessage().contains("Child"));
    }

    @Test
    void testFeedPet_PremiumAndDeluxeFood() {
        mockGetPetState(pet);
        child.setStars(100);
        when(childRepository.findById(childId)).thenReturn(Optional.of(child));
        when(em.merge(any(PetState.class))).thenAnswer(i -> i.getArgument(0));

        // Test premium
        petService.feedPet(childId, "premium_meat");
        assertEquals(85, child.getStars()); // 100 - 15 = 85

        // Test deluxe
        petService.feedPet(childId, "deluxe_potion");
        assertEquals(55, child.getStars()); // 85 - 30 = 55
    }

    @Test
    void testInteractPet_NoEvolution() {
        pet.setXp(10); // Safe from evolution
        mockGetPetState(pet);
        when(em.merge(any(PetState.class))).thenAnswer(i -> i.getArgument(0));

        PetState result = petService.interactPet(childId);

        assertEquals(12, result.getXp()); // Just +2
        assertEquals(1, result.getEvolutionId()); // Still 1
    }

    @Test
    void testFeedPet_DirtyData_XpToNextIsZeroAtEvolution1() {
        // 脏数据：xpToNext 为 0，应该从 evolutionId 反推为 100
        PetState dirtyPet = PetState.builder()
                .childId(childId).petName("Dino").evolutionId(1)
                .hunger(50).happiness(50).xp(10).xpToNext(0).build();
        mockGetPetState(dirtyPet);
        when(childRepository.findById(childId)).thenReturn(Optional.of(child));
        when(em.merge(any(PetState.class))).thenAnswer(i -> i.getArgument(0));

        PetState result = petService.feedPet(childId, "apple");

        // xpToNext 反推为 100，喂食后 xp=20，未触发进化
        assertEquals(100, result.getXpToNext());
        assertEquals(20, result.getXp());
        assertEquals(1, result.getEvolutionId());
    }

    @Test
    void testFeedPet_DirtyData_XpToNextIsZeroAtEvolution2() {
        // 脏数据：evolution=2 但 xpToNext 为 0，应反推为 150
        PetState dirtyPet = PetState.builder()
                .childId(childId).petName("Dino").evolutionId(2)
                .hunger(50).happiness(50).xp(10).xpToNext(0).build();
        mockGetPetState(dirtyPet);
        when(childRepository.findById(childId)).thenReturn(Optional.of(child));
        when(em.merge(any(PetState.class))).thenAnswer(i -> i.getArgument(0));

        PetState result = petService.feedPet(childId, "apple");

        assertEquals(150, result.getXpToNext()); // 1.5^1 * 100 = 150
        assertEquals(20, result.getXp()); // 10 + 10
        assertEquals(2, result.getEvolutionId()); // 未触发进化，保持 2
    }

    @Test
    void testFeedPet_DirtyData_XpToNextIsZero_TriggersEvolutionAfterFeed() {
        // 脏数据修复后，喂食刚好触发进化
        PetState dirtyPet = PetState.builder()
                .childId(childId).petName("Dino").evolutionId(1)
                .hunger(50).happiness(50).xp(95).xpToNext(0).build();
        mockGetPetState(dirtyPet);
        when(childRepository.findById(childId)).thenReturn(Optional.of(child));
        when(em.merge(any(PetState.class))).thenAnswer(i -> i.getArgument(0));

        PetState result = petService.feedPet(childId, "apple");

        // xpToNext 反推为 100，喂食后 xp=105 >= 100 → 触发进化
        assertEquals(0, result.getXp()); // 进化后重置
        assertEquals(2, result.getEvolutionId());
        assertEquals(150, result.getXpToNext()); // 进化后的新阈值 100 * 1.5
    }
}
