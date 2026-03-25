package com.kidslearn.user.service;

import com.kidslearn.user.domain.Device;
import com.kidslearn.user.dto.DeviceRegisterRequest;
import com.kidslearn.user.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceService {

    private final DeviceRepository deviceRepository;

    @Transactional
    public void registerDevice(UUID childId, DeviceRegisterRequest req) {
        var existing = deviceRepository.findByChildIdAndDeviceId(childId, req.deviceId());
        if (existing.isPresent()) {
            Device device = existing.get();
            device.setPushToken(req.pushToken());
            if (req.platform() != null) device.setPlatform(req.platform());
            deviceRepository.save(device);
            log.info("DEVICE_UPDATE | childId={} | deviceId={}", childId, req.deviceId());
        } else {
            Device device = Device.builder()
                .childId(childId)
                .deviceId(req.deviceId())
                .pushToken(req.pushToken())
                .platform(req.platform() != null ? req.platform() : "ios")
                .build();
            deviceRepository.save(device);
            log.info("DEVICE_REGISTER | childId={} | deviceId={}", childId, req.deviceId());
        }
    }
}
