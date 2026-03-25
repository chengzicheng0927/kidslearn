package com.kidslearn.content.controller;

import com.kidslearn.common.exception.ResourceNotFoundException;
import com.kidslearn.common.record.ApiResponse;
import com.kidslearn.content.domain.PetState;
import com.kidslearn.content.service.PetService;
import com.kidslearn.user.domain.Child;
import com.kidslearn.user.repository.ChildRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/children/{childId}/pet")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;
    private final ChildRepository childRepository;

    @GetMapping
    public ApiResponse<PetStateDTO> getPetState(@PathVariable UUID childId) {
        PetState pet = petService.getPetState(childId);
        // 找不到宠物记录，返回初始状态（而非 404）
        return ApiResponse.ok(pet != null ? toPetDTO(pet) : new PetStateDTO("小星星", 1, 80, 50, 0, 100, 0));
    }

    @PostMapping("/feed")
    public ApiResponse<PetStateDTO> feedPet(@PathVariable UUID childId,
                                             @RequestBody FeedPetRequest req) {
        PetState pet = petService.feedPet(childId, req.foodItemId());
        Child child = childRepository.findById(childId)
            .orElseThrow(() -> new ResourceNotFoundException("Child", childId));
        return ApiResponse.ok(toPetDTO(pet, child.getStars()));
    }

    @PostMapping("/interact")
    public ApiResponse<PetStateDTO> interactPet(@PathVariable UUID childId) {
        PetState pet = petService.interactPet(childId);
        return ApiResponse.ok(toPetDTO(pet));
    }

    private PetStateDTO toPetDTO(PetState p, int childStars) {
        // 防御：xpToNext 为 0 属于脏数据，从 evolutionId 反推正确值
        int xpToNext = p.getXpToNext();
        if (xpToNext <= 0) {
            xpToNext = (int) (100 * Math.pow(1.5, p.getEvolutionId() - 1));
        }
        return new PetStateDTO(
            p.getPetName(), p.getEvolutionId(),
            p.getHappiness(), p.getHunger(),
            p.getXp(), xpToNext,
            childStars
        );
    }

    private PetStateDTO toPetDTO(PetState p) {
        return toPetDTO(p, 0);
    }

    public record PetStateDTO(
        String name, int evolutionId,
        int happiness, int hunger,
        int xp, int xpToNext,
        int childStars
    ) {}

    public record FeedPetRequest(String foodItemId) {}
}
