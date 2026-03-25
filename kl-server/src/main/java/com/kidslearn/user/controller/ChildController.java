package com.kidslearn.user.controller;

import com.kidslearn.common.exception.ResourceNotFoundException;
import com.kidslearn.common.record.ApiResponse;
import com.kidslearn.user.domain.Child;
import com.kidslearn.user.dto.*;
import com.kidslearn.user.repository.ChildRepository;
import com.kidslearn.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/children")
@RequiredArgsConstructor
public class ChildController {

    private final ChildRepository childRepository;
    private final AuthService authService;

    @PostMapping("/register")
    public ApiResponse<ChildDTO> register(@Valid @RequestBody RegisterChildRequest req,
                                           Authentication auth) {
        UUID parentId = (UUID) auth.getPrincipal();
        Child child = Child.builder()
            .parentId(parentId)
            .name(req.name())
            .age(req.age())
            .ageGroup(req.ageGroup())
            .build();
        child = childRepository.save(child);
        return ApiResponse.ok(authService.toChildDTO(child));
    }

    @GetMapping("/{id}/profile")
    public ApiResponse<ChildDTO> getProfile(@PathVariable UUID id) {
        Child child = childRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Child", id));
        return ApiResponse.ok(authService.toChildDTO(child));
    }

    @GetMapping("/profile")
    public ApiResponse<CheckUserStatusResponse> checkProfileByDevice(@RequestParam String deviceId) {
        return ApiResponse.ok(authService.checkUserStatus(deviceId));
    }
}
