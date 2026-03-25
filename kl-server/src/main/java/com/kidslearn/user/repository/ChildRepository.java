package com.kidslearn.user.repository;

import com.kidslearn.user.domain.Child;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ChildRepository extends JpaRepository<Child, UUID> {

    List<Child> findByParentId(UUID parentId);

    @Modifying
    @Query("UPDATE Child c SET c.stars = c.stars + :stars WHERE c.id = :childId")
    void addStars(UUID childId, int stars);

    @Modifying
    @Query("UPDATE Child c SET c.xp = c.xp + :xp WHERE c.id = :childId")
    void addXp(UUID childId, int xp);

    @Modifying
    @Query("UPDATE Child c SET c.totalAnswered = c.totalAnswered + 1 WHERE c.id = :childId")
    void incrementTotalAnswered(UUID childId);
}
