package com.kidslearn.report.service;

import com.kidslearn.report.domain.StudyRecord;
import com.kidslearn.report.dto.*;
import com.kidslearn.report.repository.StudyRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CalendarService {

    private final StudyRecordRepository studyRecordRepository;

    public MonthCalendarResponse getMonthCalendar(UUID childId, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);

        List<StudyRecord> records = studyRecordRepository
            .findByChildIdAndRecordDateBetween(childId, start, end);

        Map<Integer, MonthCalendarResponse.StudyDayRecordDTO> days = new LinkedHashMap<>();

        // 按日分组
        Map<Integer, List<StudyRecord>> byDay = records.stream()
            .collect(Collectors.groupingBy(r -> r.getRecordDate().getDayOfMonth()));

        for (var entry : byDay.entrySet()) {
            int day = entry.getKey();
            List<StudyRecord> dayRecords = entry.getValue();

            int totalMinutes = dayRecords.stream().mapToInt(StudyRecord::getMinutes).sum();
            int heat = calculateHeat(totalMinutes);

            List<MonthCalendarResponse.StudySubjectRecordDTO> subjects = dayRecords.stream()
                .map(r -> new MonthCalendarResponse.StudySubjectRecordDTO(
                    "en".equals(r.getSubject()) ? "🇬🇧" : "🔢",
                    "en".equals(r.getSubject()) ? "英语" : "数学",
                    r.getTopic(),
                    r.getStarsEarned(),
                    r.getAccuracy() != null ? r.getAccuracy().intValue() : 0,
                    "en".equals(r.getSubject()) ? "#4A90D9" : "#7B61FF",
                    "en".equals(r.getSubject()) ? "#E8F0FE" : "#F0ECFF"
                ))
                .toList();

            days.put(day, new MonthCalendarResponse.StudyDayRecordDTO(heat, totalMinutes, subjects));
        }

        return new MonthCalendarResponse(year, month, days);
    }

    public CalendarStatsDTO getCalendarStats(UUID childId, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);
        LocalDate today = LocalDate.now();

        List<StudyRecord> records = studyRecordRepository
            .findByChildIdAndRecordDateBetween(childId, start, end);

        int totalQuestions = records.stream().mapToInt(StudyRecord::getQuestionCount).sum();
        int totalStars = records.stream().mapToInt(StudyRecord::getStarsEarned).sum();

        // 今日数据
        int todayTotal = 0, todayCorrect = 0;
        for (StudyRecord r : records) {
            if (r.getRecordDate().equals(today)) {
                todayTotal += r.getQuestionCount();
                todayCorrect += r.getCorrectCount();
            }
        }

        // 计算连续学习天数
        int currentStreak = calculateStreak(childId);

        return new CalendarStatsDTO(totalQuestions, currentStreak, currentStreak, todayCorrect, todayTotal, totalStars);
    }

    private int calculateStreak(UUID childId) {
        List<StudyRecord> recent = studyRecordRepository.findRecentByChildId(
            childId, LocalDate.now().minusDays(365)
        );

        Set<LocalDate> studyDates = recent.stream()
            .map(StudyRecord::getRecordDate)
            .collect(Collectors.toSet());

        int streak = 0;
        LocalDate date = LocalDate.now();
        while (studyDates.contains(date)) {
            streak++;
            date = date.minusDays(1);
        }
        return streak;
    }

    private int calculateHeat(int minutes) {
        if (minutes <= 0) return 0;
        if (minutes <= 15) return 1;
        if (minutes <= 30) return 2;
        if (minutes <= 60) return 3;
        return 4;
    }
}
