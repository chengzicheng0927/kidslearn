package com.kidslearn.user.controller;

import com.kidslearn.common.record.ApiResponse;
import com.kidslearn.user.dto.ValidateIdCardRequest;
import com.kidslearn.user.dto.ValidateIdCardResponse;
import com.kidslearn.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/idCard")
@RequiredArgsConstructor
public class IdCardController {

    private final AuthService authService;

    @PostMapping("/validate")
    public ApiResponse<ValidateIdCardResponse> validateIdCard(@Valid @RequestBody ValidateIdCardRequest request) {
        return ApiResponse.ok(authService.validateIdCard(request.idCard()));
    }
}
