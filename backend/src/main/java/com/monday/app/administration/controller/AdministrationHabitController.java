package com.monday.app.administration.controller;

import com.monday.app.administration.dto.*;
import com.monday.app.administration.service.*;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/administration/habits", "/api/administration/habits", "/api/v1/habits", "/api/habits"})
public class AdministrationHabitController {

    private final AdministrationHabitService habitService;
    private final AdministrationHabitExecutionService executionService;
    private final AdministrationHabitAnalyticsService analyticsService;
    private final AdministrationHabitInsightService insightService;
    private final AdministrationHabitReminderService reminderService;

    public AdministrationHabitController(
            AdministrationHabitService habitService,
            AdministrationHabitExecutionService executionService,
            AdministrationHabitAnalyticsService analyticsService,
            AdministrationHabitInsightService insightService,
            AdministrationHabitReminderService reminderService
    ) {
        this.habitService = habitService;
        this.executionService = executionService;
        this.analyticsService = analyticsService;
        this.insightService = insightService;
        this.reminderService = reminderService;
    }

    // ==========================================
    // 4.1 Habit Management
    // ==========================================

    @GetMapping
    public ResponseEntity<List<HabitDto>> getHabits(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) UUID groupId,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(habitService.getHabits(status, category, groupId, search));
    }

    @GetMapping("/{id}")
    public ResponseEntity<HabitDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(habitService.getById(id));
    }

    @PostMapping
    public ResponseEntity<HabitDto> create(@Valid @RequestBody CreateHabitRequest req) {
        HabitDto created = habitService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<HabitDto> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateHabitRequest req
    ) {
        return ResponseEntity.ok(habitService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        habitService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<Void> pause(
            @PathVariable UUID id,
            @RequestBody(required = false) HabitPauseRequest req
    ) {
        habitService.pause(id, req);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<Void> resume(@PathVariable UUID id) {
        habitService.resume(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<Void> archive(@PathVariable UUID id) {
        habitService.archive(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/unarchive")
    public ResponseEntity<Void> unarchive(@PathVariable UUID id) {
        habitService.unarchive(id);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // 5.1 & 4.2 Page Overview & Execution
    // ==========================================

    @GetMapping("/overview")
    public ResponseEntity<HabitOverviewDto> getOverview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        TodayHabitViewDto today = executionService.getTodayView(targetDate);
        WeekHabitViewDto week = executionService.getWeekView(targetDate);
        List<HabitDto> activeHabits = habitService.getHabits("ACTIVE", null, null, null);

        List<StreakHighlightDto> streakHighlights = activeHabits.stream()
                .filter(h -> h.currentStreak() > 0)
                .sorted(Comparator.comparingInt(HabitDto::currentStreak).reversed())
                .limit(5)
                .map(h -> new StreakHighlightDto(h.id(), h.name(), h.category(), h.currentStreak(), h.longestStreak()))
                .toList();

        List<HabitInsightDto> insights = insightService.generateInsights();

        return ResponseEntity.ok(new HabitOverviewDto(
                today,
                week,
                activeHabits.size(),
                streakHighlights,
                insights
        ));
    }

    @GetMapping("/today")
    public ResponseEntity<TodayHabitViewDto> getToday(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(executionService.getTodayView(date));
    }

    @GetMapping("/week")
    public ResponseEntity<WeekHabitViewDto> getWeek(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(executionService.getWeekView(date));
    }

    @PostMapping("/occurrences/{id}/complete")
    public ResponseEntity<HabitOccurrenceDto> completeOccurrence(
            @PathVariable UUID id,
            @RequestBody(required = false) CompleteOccurrenceRequest req
    ) {
        return ResponseEntity.ok(executionService.completeOccurrence(id, req));
    }

    @PostMapping("/occurrences/{id}/partial")
    public ResponseEntity<HabitOccurrenceDto> partialOccurrence(
            @PathVariable UUID id,
            @RequestBody(required = false) PartialOccurrenceRequest req
    ) {
        return ResponseEntity.ok(executionService.partialOccurrence(id, req));
    }

    @PostMapping("/occurrences/{id}/skip")
    public ResponseEntity<HabitOccurrenceDto> skipOccurrence(
            @PathVariable UUID id,
            @RequestBody(required = false) SkipOccurrenceRequest req
    ) {
        return ResponseEntity.ok(executionService.skipOccurrence(id, req));
    }

    @PostMapping("/occurrences/{id}/undo")
    public ResponseEntity<HabitOccurrenceDto> undoOccurrence(@PathVariable UUID id) {
        return ResponseEntity.ok(executionService.undoOccurrence(id));
    }

    // Flexible Target Logs
    @PostMapping("/logs")
    public ResponseEntity<HabitLogDto> createLog(@Valid @RequestBody CreateHabitLogRequest req) {
        HabitLogDto log = executionService.createLog(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(log);
    }

    @PutMapping("/logs/{id}")
    public ResponseEntity<HabitLogDto> updateLog(
            @PathVariable UUID id,
            @RequestBody UpdateHabitLogRequest req
    ) {
        return ResponseEntity.ok(executionService.updateLog(id, req));
    }

    @DeleteMapping("/logs/{id}")
    public ResponseEntity<Void> deleteLog(@PathVariable UUID id) {
        executionService.deleteLog(id);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // 4.3 Calendar, Analytics & Reports
    // ==========================================

    @GetMapping("/calendar")
    public ResponseEntity<List<CalendarDayDto>> getCalendar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        LocalDate start;
        LocalDate end;
        if (from != null && to != null) {
            start = from;
            end = to;
        } else {
            int y = year != null ? year : LocalDate.now().getYear();
            int m = month != null ? month : LocalDate.now().getMonthValue();
            YearMonth ym = YearMonth.of(y, m);
            start = ym.atDay(1);
            end = ym.atEndOfMonth();
        }
        return ResponseEntity.ok(analyticsService.getCalendarDays(start, end));
    }

    @GetMapping("/calendar/{date}")
    public ResponseEntity<CalendarDateDetailDto> getCalendarDateDetail(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(analyticsService.getCalendarDateDetail(date));
    }

    @GetMapping("/{id}/analytics")
    public ResponseEntity<HabitAnalyticsDto> getAnalytics(@PathVariable UUID id) {
        return ResponseEntity.ok(analyticsService.getHabitAnalytics(id));
    }

    @GetMapping("/reports/weekly")
    public ResponseEntity<HabitWeeklyReportDto> getWeeklyReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(analyticsService.getWeeklyReport(date));
    }

    @GetMapping("/reports/monthly")
    public ResponseEntity<HabitMonthlyReportDto> getMonthlyReport(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        int y = year != null ? year : LocalDate.now().getYear();
        int m = month != null ? month : LocalDate.now().getMonthValue();
        return ResponseEntity.ok(analyticsService.getMonthlyReport(y, m));
    }

    @GetMapping("/insights")
    public ResponseEntity<List<HabitInsightDto>> getInsights() {
        return ResponseEntity.ok(insightService.generateInsights());
    }

    @GetMapping("/digest")
    public ResponseEntity<DailyHabitDigestDto> getDailyDigest(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(reminderService.getDailyDigest(date));
    }

    // ==========================================
    // Reminders
    // ==========================================

    @GetMapping("/{id}/reminders")
    public ResponseEntity<List<HabitReminderDto>> getReminders(@PathVariable UUID id) {
        return ResponseEntity.ok(habitService.getReminders(id));
    }

    @PutMapping("/{id}/reminders")
    public ResponseEntity<List<HabitReminderDto>> saveReminders(
            @PathVariable UUID id,
            @RequestBody SaveHabitRemindersRequest req
    ) {
        return ResponseEntity.ok(habitService.saveReminders(id, req));
    }
}
