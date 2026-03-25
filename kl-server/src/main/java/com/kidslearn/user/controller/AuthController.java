package com.kidslearn.user.controller;

import com.kidslearn.common.record.ApiResponse;
import com.kidslearn.user.dto.*;
import com.kidslearn.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/auth/validate-code")
    public ApiResponse<ValidateInviteCodeResponse> validateCode(@RequestBody ValidateInviteCodeRequest req) {
        return ApiResponse.ok(authService.validateCode(req.code()));
    }

    @PostMapping("/children/activate")
    public ApiResponse<ActivateResponse> activate(@Valid @RequestBody ActivateRequest req) {
        return ApiResponse.ok(authService.activate(req));
    }

    @PostMapping("/auth/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return ApiResponse.ok(authService.login(req));
    }

    @GetMapping("/invite/validate")
    public ApiResponse<ValidateInviteCodeResponse> validateInviteCode(@RequestParam String code) {
        return ApiResponse.ok(authService.validateCode(code));
    }
}
