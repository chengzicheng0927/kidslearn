package com.kidslearn.user.repository;

import com.kidslearn.user.domain.ParentSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ParentSettingsRepository extends JpaRepository<ParentSettings, UUID> {

    Optional<ParentSettings> findByParentId(UUID parentId);
}
