package com.kidslearn.report.repository;

import com.kidslearn.report.domain.StudyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudyRecordRepository extends JpaRepository<StudyRecord, UUID> {

    List<StudyRecord> findByChildIdAndRecordDateBetween(UUID childId, LocalDate start, LocalDate end);

    Optional<StudyRecord> findByChildIdAndRecordDateAndSubject(UUID childId, LocalDate date, String subject);

    @Query("SELECT sr FROM StudyRecord sr WHERE sr.childId = :childId AND sr.recordDate >= :start ORDER BY sr.recordDate DESC")
    List<StudyRecord> findRecentByChildId(UUID childId, LocalDate start);
}
