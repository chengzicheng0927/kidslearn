package com.kidslearn.user.controller;

import com.kidslearn.common.record.ApiResponse;
import com.kidslearn.user.dto.*;
import com.kidslearn.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/parent/password")
@RequiredArgsConstructor
public class PasswordController {

    private final AuthService authService;

    @GetMapping("/status")
    public ApiResponse<PasswordStatusResponse> getStatus(Authentication auth) {
        UUID parentId = (UUID) auth.getPrincipal();
        return ApiResponse.ok(authService.getPasswordStatus(parentId));
    }

    @PostMapping("/set")
    public ApiResponse<Void> setPassword(@Valid @RequestBody SetPasswordRequest req,
                                          Authentication auth) {
        UUID parentId = (UUID) auth.getPrincipal();
        authService.setPassword(parentId, req.password());
        return ApiResponse.ok();
    }

    @PostMapping("/verify")
    public ApiResponse<VerifyPasswordResponse> verify(@Valid @RequestBody VerifyPasswordRequest req,
                                                       Authentication auth) {
        UUID parentId = (UUID) auth.getPrincipal();
        return ApiResponse.ok(authService.verifyPassword(parentId, req.password()));
    }
}
