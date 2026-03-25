package com.kidslearn.user.controller;

import com.kidslearn.common.record.ApiResponse;
import com.kidslearn.user.dto.DeviceRegisterRequest;
import com.kidslearn.user.service.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/device")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody DeviceRegisterRequest req,
                                       @RequestParam UUID childId) {
        deviceService.registerDevice(childId, req);
        return ApiResponse.ok();
    }
}
