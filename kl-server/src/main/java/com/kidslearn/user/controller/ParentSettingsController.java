package com.kidslearn.user.controller;

import com.kidslearn.common.exception.ResourceNotFoundException;
import com.kidslearn.common.record.ApiResponse;
import com.kidslearn.user.domain.ParentSettings;
import com.kidslearn.user.dto.*;
import com.kidslearn.user.repository.ParentSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/parent/settings")
@RequiredArgsConstructor
public class ParentSettingsController {

    private final ParentSettingsRepository settingsRepository;

    @GetMapping
    public ApiResponse<ParentSettingsDTO> getSettings(Authentication auth) {
        UUID parentId = (UUID) auth.getPrincipal();
        ParentSettings s = settingsRepository.findByParentId(parentId)
            .orElseThrow(() -> new ResourceNotFoundException("ParentSettings", parentId));
        return ApiResponse.ok(toDTO(s));
    }

    @PutMapping
    public ApiResponse<ParentSettingsDTO> updateSettings(@RequestBody UpdateParentSettingsRequest req,
                                                          Authentication auth) {
        UUID parentId = (UUID) auth.getPrincipal();
        ParentSettings s = settingsRepository.findByParentId(parentId)
            .orElseThrow(() -> new ResourceNotFoundException("ParentSettings", parentId));

        if (req.dailyLimitMinutes() != null) s.setDailyLimitMinutes(req.dailyLimitMinutes());
        if (req.dndEnabled() != null) s.setDndEnabled(req.dndEnabled());
        if (req.dndStart() != null) s.setDndStart(req.dndStart());
        if (req.dndEnd() != null) s.setDndEnd(req.dndEnd());
        if (req.eyeCareReminder() != null) s.setEyeCareReminder(req.eyeCareReminder());
        if (req.breakReminder() != null) s.setBreakReminder(req.breakReminder());
        if (req.teacherName() != null) s.setTeacherName(req.teacherName());

        s = settingsRepository.save(s);
        return ApiResponse.ok(toDTO(s));
    }

    private ParentSettingsDTO toDTO(ParentSettings s) {
        return new ParentSettingsDTO(
            s.getDailyLimitMinutes(), s.isDndEnabled(),
            s.getDndStart(), s.getDndEnd(),
            s.isEyeCareReminder(), s.isBreakReminder(),
            s.getTeacherName()
        );
    }
}
