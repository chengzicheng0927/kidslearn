package com.kidslearn.report.dto;

import java.util.List;

public record DayStudyDetailResponse(
    String date,
    int totalMinutes,
    int totalQuestions,
    int correctCount,
    int accuracy,
    int streak,
    List<MonthCalendarResponse.StudySubjectRecordDTO> subjects
) {}
