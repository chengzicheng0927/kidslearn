package com.kidslearn.content.controller;

import com.kidslearn.common.record.ApiResponse;
import com.kidslearn.content.dto.BadgeDTO;
import com.kidslearn.content.service.BadgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/children/{childId}/badges")
@RequiredArgsConstructor
public class BadgeController {

    private final BadgeService badgeService;

    @GetMapping
    public ApiResponse<BadgeDTO> getBadges(@PathVariable UUID childId) {
        return ApiResponse.ok(badgeService.getBadges(childId));
    }
}
