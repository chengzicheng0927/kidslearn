package com.kidslearn.content.controller;

import com.kidslearn.common.record.ApiResponse;
import com.kidslearn.content.dto.AchievementDTO;
import com.kidslearn.content.service.AchievementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/children/{childId}/achievements")
@RequiredArgsConstructor
public class AchievementController {

    private final AchievementService achievementService;

    @GetMapping
    public ApiResponse<AchievementDTO> getAchievements(@PathVariable UUID childId) {
        return ApiResponse.ok(achievementService.getAchievements(childId));
    }
}
