package com.kidslearn.report.controller;

import com.kidslearn.common.record.ApiResponse;
import com.kidslearn.report.dto.*;
import com.kidslearn.report.service.CalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/children/{childId}/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;

    @GetMapping("/{year}/{month}")
    public ApiResponse<MonthCalendarResponse> getMonthCalendar(
        @PathVariable UUID childId,
        @PathVariable int year,
        @PathVariable int month
    ) {
        return ApiResponse.ok(calendarService.getMonthCalendar(childId, year, month));
    }

    @GetMapping("/daily/{date}")
    public ApiResponse<DayStudyDetailResponse> getDayStudyDetail(
        @PathVariable UUID childId,
        @PathVariable LocalDate date
    ) {
        return ApiResponse.ok(calendarService.getDayStudyDetail(childId, date));
    }

    @GetMapping("/stats")
    public ApiResponse<CalendarStatsDTO> getStats(
        @PathVariable UUID childId,
        @RequestParam(defaultValue = "2026") int year,
        @RequestParam(defaultValue = "3") int month
    ) {
        return ApiResponse.ok(calendarService.getCalendarStats(childId, year, month));
    }
}
