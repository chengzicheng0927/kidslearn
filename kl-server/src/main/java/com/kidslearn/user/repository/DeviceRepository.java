package com.kidslearn.user.repository;

import com.kidslearn.user.domain.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceRepository extends JpaRepository<Device, UUID> {

    List<Device> findByChildId(UUID childId);

    Optional<Device> findByDeviceId(String deviceId);

    Optional<Device> findByChildIdAndDeviceId(UUID childId, String deviceId);
}
