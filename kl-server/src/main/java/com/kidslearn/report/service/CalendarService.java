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
                    getSubjectIcon(r.getSubject()),
                    getSubjectName(r.getSubject()),
                    r.getTopic(),
                    r.getStarsEarned(),
                    r.getAccuracy() != null ? r.getAccuracy().intValue() : 0,
                    getSubjectColor(r.getSubject()),
                    getSubjectBg(r.getSubject())
                ))
                .toList();

            days.put(day, new MonthCalendarResponse.StudyDayRecordDTO(heat, totalMinutes, subjects));
        }

        return new MonthCalendarResponse(year, month, days);
    }

    public CalendarStatsDTO getCalendarStats(UUID childId, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);

        List<StudyRecord> records = studyRecordRepository
            .findByChildIdAndRecordDateBetween(childId, start, end);

        Set<LocalDate> studyDates = records.stream()
            .map(StudyRecord::getRecordDate)
            .collect(Collectors.toSet());
        int totalDays = studyDates.size();
        int totalMinutes = records.stream().mapToInt(StudyRecord::getMinutes).sum();
        int totalQuestions = records.stream().mapToInt(StudyRecord::getQuestionCount).sum();
        int totalStars = records.stream().mapToInt(StudyRecord::getStarsEarned).sum();

        int currentStreak = calculateStreak(childId);
        int longestStreak = calculateLongestStreak(childId);

        return new CalendarStatsDTO(totalDays, totalMinutes, totalQuestions, totalStars, currentStreak, longestStreak);
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

    public DayStudyDetailResponse getDayStudyDetail(UUID childId, LocalDate date) {
        List<StudyRecord> records = studyRecordRepository.findByChildIdAndRecordDate(childId, date);

        int totalMinutes = records.stream().mapToInt(StudyRecord::getMinutes).sum();
        int totalQuestions = records.stream().mapToInt(StudyRecord::getQuestionCount).sum();
        int correctCount = records.stream().mapToInt(StudyRecord::getCorrectCount).sum();
        int accuracy = totalQuestions > 0 ? Math.round((float) correctCount / totalQuestions * 100) : 0;
        int streak = calculateStreak(childId);

        List<MonthCalendarResponse.StudySubjectRecordDTO> subjects = records.stream()
            .map(r -> new MonthCalendarResponse.StudySubjectRecordDTO(
                getSubjectIcon(r.getSubject()),
                getSubjectName(r.getSubject()),
                r.getTopic(),
                r.getStarsEarned(),
                r.getAccuracy() != null ? r.getAccuracy().intValue() : 0,
                getSubjectColor(r.getSubject()),
                getSubjectBg(r.getSubject())
            ))
            .toList();

        return new DayStudyDetailResponse(date.toString(), totalMinutes, totalQuestions, correctCount, accuracy, streak, subjects);
    }

    private int calculateLongestStreak(UUID childId) {
        List<StudyRecord> recent = studyRecordRepository.findRecentByChildId(
            childId, LocalDate.now().minusDays(365)
        );

        List<LocalDate> sortedDates = recent.stream()
            .map(StudyRecord::getRecordDate)
            .distinct()
            .sorted()
            .toList();

        if (sortedDates.isEmpty()) return 0;

        int longest = 1, current = 1;
        for (int i = 1; i < sortedDates.size(); i++) {
            if (sortedDates.get(i).minusDays(1).equals(sortedDates.get(i - 1))) {
                current++;
                longest = Math.max(longest, current);
            } else {
                current = 1;
            }
        }
        return longest;
    }

    private int calculateHeat(int minutes) {
        if (minutes <= 0) return 0;
        if (minutes <= 15) return 1;
        if (minutes <= 30) return 2;
        if (minutes <= 60) return 3;
        return 4;
    }

    private String getSubjectIcon(String subject) {
        return "en".equals(subject) ? "📖" : "🔢";
    }

    private String getSubjectName(String subject) {
        return "en".equals(subject) ? "英语" : "数学";
    }

    private String getSubjectColor(String subject) {
        return "en".equals(subject) ? "#40C4FF" : "#69F0AE";
    }

    private String getSubjectBg(String subject) {
        return "en".equals(subject) ? "rgba(64,196,255,.12)" : "rgba(105,240,174,.12)";
    }
}
