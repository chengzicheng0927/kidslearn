package com.kidslearn.report.dto;

import java.util.List;
import java.util.Map;

public record MonthCalendarResponse(int year, int month, Map<Integer, StudyDayRecordDTO> days) {
    public record StudyDayRecordDTO(int heat, int minutes, List<StudySubjectRecordDTO> subjects) {}
    public record StudySubjectRecordDTO(
        String icon, String name, String topic,
        int stars, int accuracy, String color, String bg
    ) {}
}
