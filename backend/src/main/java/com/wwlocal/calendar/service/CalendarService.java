package com.wwlocal.calendar.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CalendarService {
  public static final Set<String> CALENDAR_COLUMNS = Set.of(
      "name", "description", "type", "color", "owner_user_id", "visible");

  private final CrudService crud;
  private final JdbcTemplate jdbc;
  private final AuditService audit;

  public CalendarService(CrudService crud, JdbcTemplate jdbc, AuditService audit) {
    this.crud = crud;
    this.jdbc = jdbc;
    this.audit = audit;
  }

  public List<Map<String, Object>> listAllCalendars() {
    return jdbc.queryForList("""
        SELECT c.*,
          COALESCE(json_agg(DISTINCT jsonb_build_object('id', s.id, 'scopeType', s.scope_type, 'departmentId', s.department_id, 'userId', s.user_id))
            FILTER (WHERE s.id IS NOT NULL), '[]') AS scopes,
          COALESCE(json_agg(DISTINCT jsonb_build_object('id', u.id, 'name', u.name))
            FILTER (WHERE u.id IS NOT NULL), '[]') AS shared_members
        FROM calendars c
        LEFT JOIN calendar_auto_subscribe_scope s ON s.calendar_id = c.id
        LEFT JOIN calendar_shared_member m ON m.calendar_id = c.id
        LEFT JOIN users u ON u.id = m.user_id
        WHERE c.type = 'ALL_MEMBER'
        GROUP BY c.id
        ORDER BY c.id DESC
        """);
  }

  @Transactional
  public Map<String, Object> saveAllCalendar(Map<String, Object> payload) {
    var id = payload.get("id");
    payload.put("type", "ALL_MEMBER");
    payload.putIfAbsent("visible", true);
    Map<String, Object> calendar = id == null
        ? crud.create("calendars", CALENDAR_COLUMNS, payload)
        : crud.update("calendars", CALENDAR_COLUMNS, ((Number) id).longValue(), payload);
    long calendarId = ((Number) calendar.get("id")).longValue();
    replaceScopes(calendarId, (List<?>) payload.get("scopes"));
    replaceMembers(calendarId, (List<?>) payload.get("sharedMemberIds"));
    audit.record(number(payload.get("operatorUserId")), "ALL_CALENDAR", id == null ? "CREATE" : "UPDATE",
        "calendars", calendarId, "全员日历已保存");
    return calendar;
  }

  @Transactional
  public void disableAllCalendar(long id, String operatorUserId) {
    jdbc.update("UPDATE calendars SET visible = false, updated_at = now() WHERE id = ?", id);
    audit.record(operatorUserId, "ALL_CALENDAR", "DISABLE", "calendars", id, "全员日历已停用");
  }

  private void replaceScopes(long calendarId, List<?> scopes) {
    jdbc.update("DELETE FROM calendar_auto_subscribe_scope WHERE calendar_id = ?", calendarId);
    if (scopes == null || scopes.isEmpty()) {
      jdbc.update("INSERT INTO calendar_auto_subscribe_scope(calendar_id, scope_type) VALUES (?, 'COMPANY')", calendarId);
      return;
    }
    for (Object item : scopes) {
      if (item instanceof Map<?, ?> scope) {
        jdbc.update("""
            INSERT INTO calendar_auto_subscribe_scope(calendar_id, scope_type, department_id, user_id)
            VALUES (?, ?, ?, ?)
            """, calendarId, scope.get("scopeType"), scope.get("departmentId"), scope.get("userId"));
      }
    }
  }

  private void replaceMembers(long calendarId, List<?> memberIds) {
    jdbc.update("DELETE FROM calendar_shared_member WHERE calendar_id = ?", calendarId);
    if (memberIds == null) {
      return;
    }
    for (Object value : memberIds) {
      jdbc.update("INSERT INTO calendar_shared_member(calendar_id, user_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
          calendarId, String.valueOf(value));
    }
  }

  private String number(Object value) {
    return value instanceof String s ? s : null;
  }
}
