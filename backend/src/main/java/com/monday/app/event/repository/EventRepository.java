package com.monday.app.event.repository;

import com.monday.app.event.entity.Event;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class EventRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public EventRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Event save(Event event) {
        UUID id = event.getId() != null ? event.getId() : UUID.randomUUID();
        String sql = """
                INSERT INTO events (id, title, description, event_type, start_at, end_at,
                    all_day, is_recurring, recurrence_pattern, location, status,
                    requires_prep, prep_title, prep_due_text)
                VALUES (:id, :title, :description, :eventType, :startAt, :endAt,
                    :allDay, :isRecurring, :recurrencePattern, :location, :status,
                    :requiresPrep, :prepTitle, :prepDueText)
                RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(id, event), EventRowMapper.INSTANCE);
    }

    public Event update(Event event) {
        String sql = """
                UPDATE events SET
                    title = :title, description = :description, event_type = :eventType,
                    start_at = :startAt, end_at = :endAt, all_day = :allDay,
                    is_recurring = :isRecurring, recurrence_pattern = :recurrencePattern,
                    location = :location, status = :status,
                    requires_prep = :requiresPrep, prep_title = :prepTitle, prep_due_text = :prepDueText
                WHERE id = :id
                RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(event.getId(), event), EventRowMapper.INSTANCE);
    }

    public Optional<Event> findById(UUID id) {
        return jdbc.query("SELECT * FROM events WHERE id = :id",
                new MapSqlParameterSource("id", id), EventRowMapper.INSTANCE).stream().findFirst();
    }

    public List<Event> findAll() {
        return jdbc.query("SELECT * FROM events ORDER BY start_at", EventRowMapper.INSTANCE);
    }

    public List<Event> findUpcoming(Instant from, Instant to) {
        String sql = """
                SELECT * FROM events
                WHERE start_at >= :from AND start_at <= :to
                  AND status != 'CANCELLED'
                ORDER BY start_at
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("from", Timestamp.from(from))
                .addValue("to", Timestamp.from(to));
        return jdbc.query(sql, params, EventRowMapper.INSTANCE);
    }

    public void deleteById(UUID id) {
        jdbc.update("DELETE FROM events WHERE id = :id", new MapSqlParameterSource("id", id));
    }

    public List<java.util.Map<String, Object>> getPrepItemsForEvent(UUID eventId) {
        String sql = "SELECT id, text, completed, due FROM event_prep_items WHERE event_id = :eventId ORDER BY created_at ASC";
        return jdbc.queryForList(sql, new MapSqlParameterSource("eventId", eventId));
    }

    public void savePrepItems(UUID eventId, List<String> prepTasks) {
        if (prepTasks == null) return;
        // Clean existing
        jdbc.update("DELETE FROM event_prep_items WHERE event_id = :eventId", new MapSqlParameterSource("eventId", eventId));
        String insertSql = "INSERT INTO event_prep_items (id, event_id, text, completed) VALUES (:id, :eventId, :text, false)";
        for (String taskText : prepTasks) {
            if (taskText != null && !taskText.trim().isEmpty()) {
                MapSqlParameterSource params = new MapSqlParameterSource()
                        .addValue("id", UUID.randomUUID())
                        .addValue("eventId", eventId)
                        .addValue("text", taskText.trim());
                jdbc.update(insertSql, params);
            }
        }
    }

    private MapSqlParameterSource toParams(UUID id, Event e) {
        return new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("title", e.getTitle())
                .addValue("description", e.getDescription())
                .addValue("eventType", e.getEventType())
                .addValue("startAt", Timestamp.from(e.getStartAt()))
                .addValue("endAt", e.getEndAt() != null ? Timestamp.from(e.getEndAt()) : null)
                .addValue("allDay", e.isAllDay())
                .addValue("isRecurring", e.isRecurring())
                .addValue("recurrencePattern", e.getRecurrencePattern())
                .addValue("location", e.getLocation())
                .addValue("status", e.getStatus())
                .addValue("requiresPrep", e.isRequiresPrep())
                .addValue("prepTitle", e.getPrepTitle())
                .addValue("prepDueText", e.getPrepDueText());
    }

    static final class EventRowMapper implements RowMapper<Event> {
        static final EventRowMapper INSTANCE = new EventRowMapper();

        @Override
        public Event mapRow(ResultSet rs, int rowNum) throws SQLException {
            Event event = new Event(
                    UUID.fromString(rs.getString("id")),
                    rs.getString("title"),
                    rs.getString("description"),
                    rs.getString("event_type"),
                    rs.getTimestamp("start_at").toInstant(),
                    rs.getTimestamp("end_at") != null ? rs.getTimestamp("end_at").toInstant() : null,
                    rs.getBoolean("all_day"),
                    rs.getBoolean("is_recurring"),
                    rs.getString("recurrence_pattern"),
                    rs.getString("location"),
                    rs.getString("status"),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("updated_at").toInstant()
            );
            try {
                event.setRequiresPrep(rs.getBoolean("requires_prep"));
                event.setPrepTitle(rs.getString("prep_title"));
                event.setPrepDueText(rs.getString("prep_due_text"));
            } catch (SQLException ignored) {}
            return event;
        }
    }

    // --- DB queries for API Endpoints ---
    public java.util.Map<String, Object> getPulse(String month, String tz) {
        ZoneId zoneId;
        try {
            zoneId = (tz != null && !tz.isBlank()) ? ZoneId.of(tz) : ZoneId.systemDefault();
        } catch (Exception e) {
            zoneId = ZoneId.systemDefault();
        }

        YearMonth ym;
        try {
            ym = (month != null && !month.isBlank()) ? YearMonth.parse(month) : YearMonth.now(zoneId);
        } catch (Exception e) {
            ym = YearMonth.now(zoneId);
        }

        LocalDate today = LocalDate.now(zoneId);
        Instant startOfToday = today.atStartOfDay(zoneId).toInstant();
        Instant startOfTomorrow = today.plusDays(1).atStartOfDay(zoneId).toInstant();
        Instant startOfMonth = ym.atDay(1).atStartOfDay(zoneId).toInstant();
        Instant startOfNextMonth = ym.plusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant();
        final String activeCycle = ym.toString();

        String sql = """
                SELECT
                  COUNT(*) FILTER (WHERE start_at >= :startOfToday AND start_at < :startOfTomorrow) AS events_today,
                  COUNT(*) FILTER (WHERE start_at >= :startOfMonth AND start_at < :startOfNextMonth) AS upcoming_this_month,
                  COUNT(*) FILTER (WHERE requires_prep = true AND start_at >= :startOfToday) AS require_prep_count
                FROM events
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("startOfToday", Timestamp.from(startOfToday))
                .addValue("startOfTomorrow", Timestamp.from(startOfTomorrow))
                .addValue("startOfMonth", Timestamp.from(startOfMonth))
                .addValue("startOfNextMonth", Timestamp.from(startOfNextMonth));

        return jdbc.queryForObject(sql, params, (rs, rowNum) -> java.util.Map.of(
                "eventsToday", rs.getInt("events_today"),
                "upcomingThisMonth", rs.getInt("upcoming_this_month"),
                "requirePrepCount", rs.getInt("require_prep_count"),
                "activeCycle", activeCycle
        ));
    }

    public List<java.util.Map<String, Object>> getDayEvents(String date) {
        LocalDate localDate;
        try {
            localDate = LocalDate.parse(date);
        } catch (Exception e) {
            localDate = LocalDate.now();
        }

        Instant start = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = localDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        String sql = """
            SELECT id, title, to_char(start_at, 'HH24:MI') as time, 
                   '60m' as duration, event_type as category, location, 'Zoom' as platform, 
                   1 as attendeesCount, 'N/A' as attendeesNames
            FROM events 
            WHERE start_at >= :start AND start_at < :end
            ORDER BY start_at
        """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("start", Timestamp.from(start))
                .addValue("end", Timestamp.from(end));

        return jdbc.queryForList(sql, params);
    }

    public List<java.util.Map<String, Object>> getPrepQueue(String status, int limit) {
        String sql = """
            SELECT id, title, to_char(start_at, 'YYYY-MM-DD') as date, 
                   prep_title as "prepTitle", prep_due_text as "prepDueText"
            FROM events 
            WHERE requires_prep = true
            ORDER BY start_at LIMIT :limit
        """;
        List<java.util.Map<String, Object>> events = jdbc.queryForList(sql, new MapSqlParameterSource("limit", limit));
        
        for (java.util.Map<String, Object> e : events) {
            String itemsSql = "SELECT id, text, completed, due FROM event_prep_items WHERE event_id = :eventId::uuid";
            List<java.util.Map<String, Object>> items = jdbc.queryForList(itemsSql, new MapSqlParameterSource("eventId", e.get("id")));
            e.put("prepChecklist", items);
        }
        return events;
    }

    public java.util.Map<String, Object> togglePrepItem(String eventId, String itemId, boolean completed) {
        String sql = "UPDATE event_prep_items SET completed = :completed WHERE id = :itemId::uuid AND event_id = :eventId::uuid RETURNING *";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("completed", completed)
                .addValue("itemId", itemId)
                .addValue("eventId", eventId);
        jdbc.queryForList(sql, params); // Execute update
        
        // check if all completed
        String checkSql = "SELECT count(*) FROM event_prep_items WHERE event_id = :eventId::uuid AND completed = false";
        int notCompleted = jdbc.queryForObject(checkSql, new MapSqlParameterSource("eventId", eventId), Integer.class);
        
        return java.util.Map.of(
                "itemId", itemId,
                "completed", completed,
                "allPrepCompleted", notCompleted == 0
        );
    }
}
