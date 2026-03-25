package com.kidslearn.content.service;

import com.kidslearn.common.exception.ResourceNotFoundException;
import com.kidslearn.content.domain.PetState;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import com.kidslearn.user.domain.Child;
import com.kidslearn.user.repository.ChildRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class PetService {

    private final EntityManager em;
    private final ChildRepository childRepository;

    public PetState getPetState(UUID childId) {
        var query = em.createQuery(
            "SELECT p FROM PetState p WHERE p.childId = :childId", PetState.class);
        query.setParameter("childId", childId);
        var results = query.getResultList();
        return results.isEmpty() ? null : results.getFirst();
    }

    @Transactional
    public PetState feedPet(UUID childId, String foodItemId) {
        Child child = childRepository.findById(childId)
            .orElseThrow(() -> new ResourceNotFoundException("Child", childId));

        PetState pet = getPetState(childId);

        // 没有宠物记录则创建初始状态
        boolean isNew = false;
        if (pet == null) {
            pet = PetState.builder()
                .childId(childId)
                .petName("小星星")
                .evolutionId(1)
                .happiness(80)
                .hunger(50)
                .xp(0)
                .xpToNext(100)
                .lastFedAt(Instant.now())
                .build();
            em.persist(pet);
            isNew = true;
        }

        int cost = 5; // Default cost
        if (foodItemId != null) {
            if (foodItemId.contains("premium")) cost = 15;
            else if (foodItemId.contains("deluxe")) cost = 30;
        }

        if (child.getStars() < cost) {
            throw new IllegalArgumentException("星星不足，快去答题赚取吧！");
        }
        child.setStars(child.getStars() - cost);
        childRepository.save(child);

        // 喂食逻辑
        pet.setHunger(Math.min(100, pet.getHunger() + 20));
        pet.setHappiness(Math.min(100, pet.getHappiness() + 5));
        pet.setXp(pet.getXp() + 10);
        pet.setLastFedAt(Instant.now());

        checkEvolution(pet, childId);

        // 新建实体直接返回（已由 persist 托管）；已有实体用 merge 更新
        return isNew ? pet : em.merge(pet);
    }

    @Transactional
    public PetState interactPet(UUID childId) {
        PetState pet = getPetState(childId);

        // 没有宠物记录则创建初始状态
        boolean isNew = false;
        if (pet == null) {
            pet = PetState.builder()
                .childId(childId)
                .petName("小星星")
                .evolutionId(1)
                .happiness(80)
                .hunger(50)
                .xp(0)
                .xpToNext(100)
                .lastFedAt(Instant.now())
                .build();
            em.persist(pet);
            isNew = true;
        }

        pet.setHappiness(Math.min(100, pet.getHappiness() + 15));
        pet.setXp(pet.getXp() + 2);
        checkEvolution(pet, childId);
        return isNew ? pet : em.merge(pet);
    }

    private void checkEvolution(PetState pet, UUID childId) {
        // 防御：xpToNext 为 0/NULL 属于脏数据，从 evolutionId 反推正确值
        // 进化规则：evolutionId=1 → 100，evolutionId=2 → 150，evolutionId=3 → 225...
        int xpToNext = pet.getXpToNext();
        if (xpToNext <= 0) {
            xpToNext = (int) (100 * Math.pow(1.5, pet.getEvolutionId() - 1));
            pet.setXpToNext(xpToNext);
            log.warn("PET_XP_TO_NEXT_REPAIRED | childId={} | evolutionId={} | restoredXpToNext={}",
                    childId, pet.getEvolutionId(), xpToNext);
        }
        if (pet.getXp() >= xpToNext) {
            pet.setEvolutionId(pet.getEvolutionId() + 1);
            pet.setXp(0);
            pet.setXpToNext(Math.max(100, (int) (xpToNext * 1.5)));
            log.info("PET_EVOLVED | childId={} | newEvolution={}", childId, pet.getEvolutionId());
        }
    }
}
