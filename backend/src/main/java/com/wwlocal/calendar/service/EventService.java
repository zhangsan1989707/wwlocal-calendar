package com.wwlocal.calendar.service;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventService {
  public static final Set<String> EVENT_COLUMNS = Set.of(
      "calendar_id", "organizer_user_id", "title", "location", "description", "tag", "tag_color",
      "start_time", "end_time", "all_day", "recurrence_rule", "recurrence_end", "allow_join", "status");

  private final CrudService crud;
  private final JdbcTemplate jdbc;
  private final AuditService audit;

  public EventService(CrudService crud, JdbcTemplate jdbc, AuditService audit) {
    this.crud = crud;
    this.jdbc = jdbc;
    this.audit = audit;
  }

  public List<Map<String, Object>> search(Map<String, String> params) {
    var sql = new StringBuilder("""
        SELECT e.*, c.name AS calendar_name, c.color AS calendar_color, u.name AS organizer_name
        FROM event e
        JOIN calendar c ON c.id = e.calendar_id
        JOIN sys_user u ON u.id = e.organizer_user_id
        WHERE e.status = 'ACTIVE'
        """);
    var args = new java.util.ArrayList<>();
    if (has(params, "keyword")) {
      sql.append(" AND (e.title ILIKE ? OR e.location ILIKE ? OR e.description ILIKE ?)");
      var keyword = "%" + params.get("keyword") + "%";
      args.add(keyword);
      args.add(keyword);
      args.add(keyword);
    }
    if (has(params, "calendarId")) {
      sql.append(" AND e.calendar_id = ?");
      args.add(Long.valueOf(params.get("calendarId")));
    }
    if (has(params, "tag")) {
      sql.append(" AND e.tag = ?");
      args.add(params.get("tag"));
    }
    if (has(params, "start")) {
      sql.append(" AND e.end_time >= ?");
      args.add(OffsetDateTime.parse(params.get("start")));
    }
    if (has(params, "end")) {
      sql.append(" AND e.start_time <= ?");
      args.add(OffsetDateTime.parse(params.get("end")));
    }
    sql.append(" ORDER BY e.start_time ASC LIMIT 500");
    return jdbc.queryForList(sql.toString(), args.toArray());
  }

  @Transactional
  public Map<String, Object> save(Map<String, Object> payload) {
    var id = payload.get("id");
    Map<String, Object> event = id == null
        ? crud.create("event", EVENT_COLUMNS, payload)
        : crud.update("event", EVENT_COLUMNS, ((Number) id).longValue(), payload);
    long eventId = ((Number) event.get("id")).longValue();
    replaceParticipants(eventId, (List<?>) payload.get("participantIds"));
    replaceTodos(eventId, (List<?>) payload.get("todos"));
    audit.record(number(payload.get("operatorUserId")), "EVENT", id == null ? "CREATE" : "UPDATE",
        "event", eventId, "日程已保存");
    return event;
  }

  @Transactional
  public void remove(long id, Long operatorUserId) {
    crud.disable("event", id);
    audit.record(operatorUserId, "EVENT", "DELETE", "event", id, "日程已删除");
  }

  public void respond(long eventId, long userId, String status) {
    jdbc.update("""
        INSERT INTO event_participant(event_id, user_id, response_status)
        VALUES (?, ?, ?)
        ON CONFLICT(event_id, user_id) DO UPDATE SET response_status = EXCLUDED.response_status, updated_at = now()
        """, eventId, userId, status);
    audit.record(userId, "EVENT", "RESPOND", "event", eventId, "参会回执已更新");
  }

  public List<Map<String, Object>> freebusy(Map<String, Object> payload) {
    var ids = (List<?>) payload.getOrDefault("userIds", List.of());
    if (ids.isEmpty()) {
      return List.of();
    }
    var marks = String.join(",", ids.stream().map(v -> "?").toList());
    var args = new java.util.ArrayList<>();
    ids.forEach(v -> args.add(((Number) v).longValue()));
    args.add(OffsetDateTime.parse(String.valueOf(payload.get("startTime"))));
    args.add(OffsetDateTime.parse(String.valueOf(payload.get("endTime"))));
    return jdbc.queryForList("""
        SELECT p.user_id, u.name, e.id AS event_id, e.title, e.start_time, e.end_time
        FROM event_participant p
        JOIN sys_user u ON u.id = p.user_id
        JOIN event e ON e.id = p.event_id
        WHERE p.user_id IN (%s)
          AND e.status = 'ACTIVE'
          AND e.end_time > ?
          AND e.start_time < ?
        ORDER BY e.start_time
        """.formatted(marks), args.toArray());
  }

  public Path exportEvents(Path exportDir, Long userId) throws Exception {
    Files.createDirectories(exportDir);
    var file = exportDir.resolve("events-" + System.currentTimeMillis() + ".xlsx");
    var events = search(Map.of());
    try (var workbook = new XSSFWorkbook(); OutputStream out = Files.newOutputStream(file)) {
      var sheet = workbook.createSheet("日程");
      var header = sheet.createRow(0);
      String[] titles = {"标题", "日历", "开始时间", "结束时间", "地点", "标签"};
      for (int i = 0; i < titles.length; i++) {
        header.createCell(i).setCellValue(titles[i]);
      }
      for (int i = 0; i < events.size(); i++) {
        var row = sheet.createRow(i + 1);
        var e = events.get(i);
        row.createCell(0).setCellValue(String.valueOf(e.get("title")));
        row.createCell(1).setCellValue(String.valueOf(e.get("calendar_name")));
        row.createCell(2).setCellValue(String.valueOf(e.get("start_time")));
        row.createCell(3).setCellValue(String.valueOf(e.get("end_time")));
        row.createCell(4).setCellValue(String.valueOf(e.get("location")));
        row.createCell(5).setCellValue(String.valueOf(e.get("tag")));
      }
      workbook.write(out);
    }
    jdbc.update("""
        INSERT INTO export_task(task_name, export_scope, status, file_path, created_by, completed_at)
        VALUES (?, 'EVENT', 'COMPLETED', ?, ?, now())
        """, "日程导出", file.toString(), userId);
    audit.record(userId, "EXPORT", "CREATE", "export_task", file.getFileName(), "日程导出文件已生成");
    return file;
  }

  private void replaceParticipants(long eventId, List<?> ids) {
    jdbc.update("DELETE FROM event_participant WHERE event_id = ?", eventId);
    if (ids != null) {
      for (Object id : ids) {
        jdbc.update("INSERT INTO event_participant(event_id, user_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
            eventId, ((Number) id).longValue());
      }
    }
  }

  private void replaceTodos(long eventId, List<?> todos) {
    jdbc.update("DELETE FROM event_todo WHERE event_id = ?", eventId);
    if (todos != null) {
      for (Object item : todos) {
        if (item instanceof Map<?, ?> todo) {
          jdbc.update("""
              INSERT INTO event_todo(event_id, title, assignee_user_id, priority, completed)
              VALUES (?, ?, ?, ?, ?)
              """, eventId, todo.get("title"), todo.get("assigneeUserId"),
              todo.containsKey("priority") ? todo.get("priority") : "MEDIUM",
              Boolean.TRUE.equals(todo.get("completed")));
        }
      }
    }
  }

  private boolean has(Map<String, String> params, String key) {
    return params.containsKey(key) && params.get(key) != null && !params.get(key).isBlank();
  }

  private Long number(Object value) {
    return value instanceof Number n ? n.longValue() : null;
  }
}
