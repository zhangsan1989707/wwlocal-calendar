package com.wwlocal.calendar.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class EventService {
  private final JdbcTemplate jdbc;
  private final CrudService crud;
  private final AuditService audit;

  public EventService(JdbcTemplate jdbc, CrudService crud, AuditService audit) {
    this.jdbc = jdbc;
    this.crud = crud;
    this.audit = audit;
  }

  // Event table columns used in search SELECT — must match database/init.sql event table
  private static final Set<String> EVENT_COLUMNS = Set.of(
      "id", "title", "description", "location", "start_at", "end_at",
      "all_day", "timezone", "visibility", "status", "tag_id",
      "calendar_id", "organizer_user_id", "created_at", "updated_at"
  );

  // Writable columns for INSERT/UPDATE on event table (must match database/init.sql event table)
  private static final Set<String> COLUMNS = Set.of(
      "title", "description", "location", "start_at", "end_at",
      "all_day", "timezone", "visibility", "status", "tag_id",
      "calendar_id", "organizer_user_id"
  );

  // Excel export columns (display names)
  private static final List<String> EXCEL_COLUMNS = List.of(
      "ID", "标题", "开始时间", "结束时间", "地点", "描述", "状态", "日历ID", "创建人", "创建时间"
  );

  /**
   * Search events with flexible filters.
   * - Without userId: shows all active events (for admin/search views)
   * - With userId: shows events the user participates in or organized
   * - Supports keyword (ILIKE on title/location/description), calendar_id, tag_id, date range
   */
  public List<Map<String, Object>> search(Map<String, String> params) {
    var select = new StringJoiner(", ");
    EVENT_COLUMNS.forEach(c -> select.add("e." + c));
    select.add("c.name AS calendar_name");
    select.add("c.color AS calendar_color");
    select.add("ct.name AS tag_name");
    select.add("ct.color AS tag_color");
    select.add("COALESCE(ep_cnt.total_participants, 0) AS total_participants");

    var sql = new StringBuilder("SELECT DISTINCT ").append(select)
        .append(" FROM event e")
        .append(" LEFT JOIN calendar c ON e.calendar_id = c.id")
        .append(" LEFT JOIN calendar_tag ct ON e.tag_id = ct.id")
        .append(" LEFT JOIN (")
        .append("  SELECT event_id, COUNT(*) AS total_participants")
        .append("  FROM event_participant GROUP BY event_id")
        .append(" ) ep_cnt ON ep_cnt.event_id = e.id");

    var args = new ArrayList<>();
    var clauses = new ArrayList<String>();

    // User-specific filter: show events where user is participant or organizer
    var userId = params.get("userId");
    if (userId != null && !userId.isBlank()) {
      sql.append(" LEFT JOIN event_participant ep ON ep.event_id = e.id");
      clauses.add("(ep.user_id = ? OR e.organizer_user_id = ?)");
      args.add(Long.parseLong(userId));
      args.add(Long.parseLong(userId));
    }

    // By default show only ACTIVE events (not CANCELLED)
    if (!params.containsKey("status")) {
      clauses.add("e.status = 'ACTIVE'");
    } else if (params.get("status") != null && !params.get("status").isBlank()) {
      clauses.add("e.status = ?");
      args.add(params.get("status"));
    }

    // Keyword search on title, location, description
    var keyword = params.get("keyword");
    if (keyword != null && !keyword.isBlank()) {
      clauses.add("(e.title ILIKE ? OR e.location ILIKE ? OR e.description ILIKE ?)");
      var pattern = "%" + keyword + "%";
      args.add(pattern);
      args.add(pattern);
      args.add(pattern);
    }

    if (params.get("calendar_id") != null && !params.get("calendar_id").isBlank()) {
      clauses.add("e.calendar_id = ?");
      args.add(Long.parseLong(params.get("calendar_id")));
    }
    // Support both 'calendarId' (from frontend CalendarSearch) and 'calendar_id'
    if (params.get("calendarId") != null && !params.get("calendarId").isBlank()) {
      clauses.add("e.calendar_id = ?");
      args.add(Long.parseLong(params.get("calendarId")));
    }
    if (params.get("tag_id") != null && !params.get("tag_id").isBlank()) {
      clauses.add("e.tag_id = ?");
      args.add(Long.parseLong(params.get("tag_id")));
    }
    if (params.get("start_at") != null && !params.get("start_at").isBlank()) {
      clauses.add("e.end_at >= ?::timestamptz");
      args.add(params.get("start_at"));
    }
    // Support 'start' as alias (from CalendarSearch date range)
    if (params.get("start") != null && !params.get("start").isBlank()) {
      clauses.add("e.end_at >= ?::timestamptz");
      args.add(params.get("start"));
    }
    if (params.get("end_at") != null && !params.get("end_at").isBlank()) {
      clauses.add("e.start_at <= ?::timestamptz");
      args.add(params.get("end_at"));
    }
    // Support 'end' as alias
    if (params.get("end") != null && !params.get("end").isBlank()) {
      clauses.add("e.start_at <= ?::timestamptz");
      args.add(params.get("end"));
    }
    if (params.get("organizer_user_id") != null && !params.get("organizer_user_id").isBlank()) {
      clauses.add("e.organizer_user_id = ?");
      args.add(Long.parseLong(params.get("organizer_user_id")));
    }

    if (!clauses.isEmpty()) {
      sql.append(" WHERE ").append(String.join(" AND ", clauses));
    }

    sql.append(" ORDER BY e.start_at");
    return jdbc.queryForList(sql.toString(), args.toArray());
  }

  /**
   * Create or update an event. After saving the event row, replaces reminders and recurrence.
   */
  public Map<String, Object> save(Map<String, Object> payload) {
    // Convert ISO string dates to Timestamp for PostgreSQL
    coerceTimestamp(payload, "start_at");
    coerceTimestamp(payload, "end_at");

    Map<String, Object> event;
    if (payload.containsKey("id") && payload.get("id") != null) {
      long id = ((Number) payload.get("id")).longValue();
      event = crud.update("event", COLUMNS, id, payload);
    } else {
      event = crud.create("event", COLUMNS, payload);
    }
    long eventId = ((Number) event.get("id")).longValue();

    // Cascading: replace reminders, participants, todos and recurrence from payload
    replaceReminders(eventId, payload);
    replaceParticipants(eventId, payload);
    replaceTodos(eventId, payload);
    replaceRecurrence(eventId, payload);

    return event;
  }

  private static void coerceTimestamp(Map<String, Object> payload, String key) {
    var val = payload.get(key);
    if (val instanceof String s && !s.isBlank()) {
      payload.put(key, Timestamp.from(java.time.OffsetDateTime.parse(s).toInstant()));
    }
  }

  /**
   * Delete existing reminders for the event, then insert from the reminders array in payload.
   */
  private void replaceReminders(long eventId, Map<String, Object> payload) {
    jdbc.update("DELETE FROM event_reminder WHERE event_id = ?", eventId);

    @SuppressWarnings("unchecked")
    var reminders = (List<Map<String, Object>>) payload.get("reminders");
    if (reminders == null || reminders.isEmpty()) {
      return;
    }
    for (var rem : reminders) {
      jdbc.update(
          "INSERT INTO event_reminder(event_id, minutes_before, method) VALUES (?, ?, ?)",
          eventId,
          rem.get("minutes_before"),
          rem.getOrDefault("method", "SYSTEM")
      );
    }
  }

  /**
   * Delete existing participants for the event, then insert from the participantIds array in payload.
   */
  private void replaceParticipants(long eventId, Map<String, Object> payload) {
    jdbc.update("DELETE FROM event_participant WHERE event_id = ?", eventId);

    @SuppressWarnings("unchecked")
    var participantIds = (List<Number>) payload.get("participantIds");
    if (participantIds == null || participantIds.isEmpty()) {
      return;
    }
    for (var pid : participantIds) {
      jdbc.update(
          "INSERT INTO event_participant(event_id, user_id, response_status) VALUES (?, ?, 'NEEDS_ACTION')",
          eventId, pid.longValue()
      );
    }
  }

  /**
   * Sync todos for the event from the todos array in payload.
   * On update: delete existing todos and re-insert. On create: just insert.
   */
  private void replaceTodos(long eventId, Map<String, Object> payload) {
    jdbc.update("DELETE FROM event_todo WHERE event_id = ?", eventId);

    @SuppressWarnings("unchecked")
    var todos = (List<Map<String, Object>>) payload.get("todos");
    if (todos == null || todos.isEmpty()) {
      return;
    }
    for (var todo : todos) {
      var title = String.valueOf(todo.getOrDefault("title", ""));
      if (title.isBlank()) continue;
      var assigneeUserId = todo.get("assigneeUserId");
      var priority = String.valueOf(todo.getOrDefault("priority", "MEDIUM"));
      var completed = Boolean.TRUE.equals(todo.get("completed"));
      jdbc.update(
          "INSERT INTO event_todo(event_id, title, assignee_user_id, priority, completed) VALUES (?, ?, ?, ?, ?)",
          eventId, title,
          assigneeUserId != null ? ((Number) assigneeUserId).longValue() : null,
          priority, completed
      );
    }
  }

  /**
   * Upsert the recurrence rule for the event.
   * If rrule is null/blank, delete any existing recurrence.
   */
  private void replaceRecurrence(long eventId, Map<String, Object> payload) {
    String rrule = (String) payload.get("rrule");
    if (rrule == null || rrule.isBlank()) {
      jdbc.update("DELETE FROM event_recurrence WHERE event_id = ?", eventId);
      return;
    }
    var existing = jdbc.queryForList(
        "SELECT id FROM event_recurrence WHERE event_id = ?", eventId);
    if (existing.isEmpty()) {
      jdbc.update(
          "INSERT INTO event_recurrence(event_id, rrule, end_at) VALUES (?, ?, ?::timestamptz)",
          eventId, rrule, payload.get("recurrence_end"));
    } else {
      jdbc.update(
          "UPDATE event_recurrence SET rrule = ?, end_at = ?::timestamptz, updated_at = now() WHERE event_id = ?",
          rrule, payload.get("recurrence_end"), eventId);
    }
  }

  /**
   * Remove (cancel) an event. Single instance: soft-delete (status = CANCELLED).
   * Series delete is not yet implemented.
   */
  public void remove(long id, Long operatorUserId, String scope) {
    if (!"single".equals(scope)) {
      throw new UnsupportedOperationException("series delete not implemented");
    }
    jdbc.update("UPDATE event SET status = 'CANCELLED', updated_at = now() WHERE id = ?", id);
    if (operatorUserId != null) {
      audit.record(operatorUserId, "event", "cancel", "event", id, "单次取消事件");
    }
  }

  /**
   * Respond to an event invitation. Upserts a row in event_participant.
   */
  public void respond(long id, long userId, String status) {
    var existing = jdbc.queryForList(
        "SELECT response_status FROM event_participant WHERE event_id = ? AND user_id = ?", id, userId);
    if (existing.isEmpty()) {
      jdbc.update("""
          INSERT INTO event_participant(event_id, user_id, response_status)
          VALUES (?, ?, ?)
          """, id, userId, status);
    } else {
      jdbc.update(
          "UPDATE event_participant SET response_status = ? WHERE event_id = ? AND user_id = ?",
          status, id, userId);
    }
  }

  /**
   * Query busy slots for a set of users in a time range.
   */
  public List<Map<String, Object>> freebusy(Map<String, Object> payload) {
    @SuppressWarnings("unchecked")
    var userIds = (List<Number>) payload.get("user_ids");
    // Support both start_at/end_at (new convention) and start_time/end_time (legacy)
    var start = String.valueOf(payload.getOrDefault("start_at", payload.get("start_time")));
    var end = String.valueOf(payload.getOrDefault("end_at", payload.get("end_time")));

    if (userIds == null || userIds.isEmpty()) {
      return List.of();
    }

    var sql = """
        SELECT e.id, e.title, e.start_at, e.end_at, e.all_day, e.status,
               COALESCE(ep.response_status, 'NEEDS_ACTION') AS response_status
        FROM event e
        JOIN event_participant ep ON ep.event_id = e.id
        WHERE e.status = 'ACTIVE'
          AND ep.user_id = ANY(?)
          AND e.start_at < ?::timestamptz
          AND e.end_at > ?::timestamptz
        ORDER BY e.start_at
        """;
    return jdbc.queryForList(sql, userIds.toArray(new Number[0]), end, start);
  }

  /**
   * Export events to an Excel-like structure and record an export_task.
   */
  public Map<String, Object> exportEvents(Long userId, String scope) {
    var sql = new StringBuilder("""
        SELECT e.id, e.title, e.start_at, e.end_at, e.location, e.description,
               e.status, e.calendar_id, e.organizer_user_id, e.created_at
        FROM event e
        JOIN event_participant ep ON ep.event_id = e.id
        WHERE ep.user_id = ?
        """);
    var args = new ArrayList<>();
    args.add(userId);

    if ("MY_EVENTS".equals(scope)) {
      sql.append(" AND ep.response_status != 'DECLINED'");
    }

    sql.append(" ORDER BY e.start_at");
    var rows = jdbc.queryForList(sql.toString(), args.toArray());

    // Record export task
    var now = Timestamp.from(Instant.now());
    jdbc.update("""
        INSERT INTO export_task(name, scope, status, created_by, file_path, started_at, finished_at)
        VALUES (?, ?, ?, ?, ?, ?, ?)
        """, "事件导出", scope, "FINISHED", userId, null, now, now);

    var result = new LinkedHashMap<String, Object>();
    result.put("columns", EXCEL_COLUMNS);
    result.put("rows", rows);
    return result;
  }

  /**
   * Export all events to an XLSX file on disk. Returns the file path.
   */
  public Path exportEvents(Path outputDir, Long userId) throws Exception {
    Files.createDirectories(outputDir);

    var sql = new StringBuilder("""
        SELECT e.id, e.title, e.start_at, e.end_at, e.location, e.description,
               e.status, e.calendar_id, e.organizer_user_id, e.created_at
        FROM event e
        LEFT JOIN event_participant ep ON ep.event_id = e.id
        """);
    var args = new ArrayList<>();
    var conditions = new ArrayList<String>();

    // Optional: filter by user (shows events the user participates in or organized)
    if (userId != null) {
      conditions.add("(ep.user_id = ? OR e.organizer_user_id = ?)");
      args.add(userId);
      args.add(userId);
    }

    if (!conditions.isEmpty()) {
      sql.append(" WHERE ").append(String.join(" AND ", conditions));
    }
    sql.append(" GROUP BY e.id ORDER BY e.start_at");

    var rows = jdbc.queryForList(sql.toString(), args.toArray());

    // Write XLSX
    var timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    var filename = "events-export-" + timestamp + ".xlsx";
    var filePath = outputDir.resolve(filename);

    try (Workbook wb = new XSSFWorkbook()) {
      Sheet sheet = wb.createSheet("日程导出");
      // Header row
      Row header = sheet.createRow(0);
      var columns = List.of("ID", "标题", "开始时间", "结束时间", "地点", "描述", "状态", "日历ID", "组织者ID", "创建时间");
      for (int i = 0; i < columns.size(); i++) {
        header.createCell(i).setCellValue(columns.get(i));
      }
      // Data rows
      var colKeys = List.of("id", "title", "start_at", "end_at", "location", "description", "status", "calendar_id", "organizer_user_id", "created_at");
      for (int r = 0; r < rows.size(); r++) {
        Row row = sheet.createRow(r + 1);
        var data = rows.get(r);
        for (int c = 0; c < colKeys.size(); c++) {
          var val = data.get(colKeys.get(c));
          if (val != null) {
            row.createCell(c).setCellValue(String.valueOf(val));
          }
        }
      }
      // Auto-size columns
      for (int i = 0; i < columns.size(); i++) {
        sheet.autoSizeColumn(i);
      }
      try (var fos = Files.newOutputStream(filePath)) {
        wb.write(fos);
      }
    }

    // Record export task
    var now = Timestamp.from(Instant.now());
    jdbc.update("""
        INSERT INTO export_task(name, scope, status, created_by, file_path, started_at, finished_at)
        VALUES (?, ?, ?, ?, ?, ?, ?)
        """, "日程全量导出", "MANAGEMENT", "FINISHED", userId, filePath.toString(), now, now);

    return filePath;
  }
}
