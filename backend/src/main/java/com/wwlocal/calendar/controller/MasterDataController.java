package com.wwlocal.calendar.controller;

import com.wwlocal.calendar.api.ApiResponse;
import com.wwlocal.calendar.service.AuditService;
import com.wwlocal.calendar.service.CalendarService;
import com.wwlocal.calendar.service.CrudService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MasterDataController {
  private static final Set<String> USER_COLUMNS = Set.of(
      "name", "department_id", "email", "mobile", "status");
  private static final Set<String> DEPARTMENT_COLUMNS = Set.of(
      "name", "parent_id", "sort_order", "enabled");
  private static final Set<String> TAG_COLUMNS = Set.of(
      "name", "color", "enabled");

  private final CrudService crud;
  private final CalendarService calendars;
  private final JdbcTemplate jdbc;
  private final AuditService audit;

  public MasterDataController(CrudService crud, CalendarService calendars, JdbcTemplate jdbc, AuditService audit) {
    this.crud = crud;
    this.calendars = calendars;
    this.jdbc = jdbc;
    this.audit = audit;
  }

  @GetMapping("/users")
  public ApiResponse<List<Map<String, Object>>> users(@RequestParam Map<String, String> params) {
    return ApiResponse.ok(crud.list("users", Set.of("department_id", "status"), params, "created_at DESC"));
  }

  @PostMapping("/users")
  public ApiResponse<Map<String, Object>> createUser(@RequestBody Map<String, Object> payload) {
    var row = crud.create("users", USER_COLUMNS, payload);
    audit.record(number(payload.get("operatorUserId")), "USER", "CREATE", "users", row.get("id"), "系统用户已新增");
    return ApiResponse.ok(row);
  }

  @PutMapping("/users/{id}")
  public ApiResponse<Map<String, Object>> updateUser(@PathVariable String id, @RequestBody Map<String, Object> payload) {
    var row = crud.update("users", USER_COLUMNS, id, payload);
    audit.record(number(payload.get("operatorUserId")), "USER", "UPDATE", "users", id, "系统用户已更新");
    return ApiResponse.ok(row);
  }

  @GetMapping("/departments")
  public ApiResponse<List<Map<String, Object>>> departments(@RequestParam Map<String, String> params) {
    return ApiResponse.ok(crud.list("departments", Set.of("enabled"), params, "sort_order ASC, id ASC"));
  }

  @PostMapping("/departments")
  public ApiResponse<Map<String, Object>> createDepartment(@RequestBody Map<String, Object> payload) {
    var row = crud.create("departments", DEPARTMENT_COLUMNS, payload);
    audit.record(number(payload.get("operatorUserId")), "DEPARTMENT", "CREATE", "departments", row.get("id"), "部门已新增");
    return ApiResponse.ok(row);
  }

  @PutMapping("/departments/{id}")
  public ApiResponse<Map<String, Object>> updateDepartment(@PathVariable String id, @RequestBody Map<String, Object> payload) {
    var row = crud.update("departments", DEPARTMENT_COLUMNS, id, payload);
    audit.record(number(payload.get("operatorUserId")), "DEPARTMENT", "UPDATE", "departments", id, "部门已更新");
    return ApiResponse.ok(row);
  }

  @GetMapping("/calendars")
  public ApiResponse<List<Map<String, Object>>> calendarList(@RequestParam Map<String, String> params) {
    return ApiResponse.ok(crud.list("calendars", Set.of("type", "visible", "owner_user_id"), params, "created_at DESC"));
  }

  @PostMapping("/calendars")
  public ApiResponse<Map<String, Object>> createCalendar(@RequestBody Map<String, Object> payload) {
    var row = crud.create("calendars", CalendarService.CALENDAR_COLUMNS, payload);
    audit.record(number(payload.get("operatorUserId")), "CALENDAR", "CREATE", "calendars", row.get("id"), "日历已新增");
    return ApiResponse.ok(row);
  }

  @PutMapping("/calendars/{id}")
  public ApiResponse<Map<String, Object>> updateCalendar(@PathVariable String id, @RequestBody Map<String, Object> payload) {
    var row = crud.update("calendars", CalendarService.CALENDAR_COLUMNS, id, payload);
    audit.record(number(payload.get("operatorUserId")), "CALENDAR", "UPDATE", "calendars", id, "日历已更新");
    return ApiResponse.ok(row);
  }

  @GetMapping("/all-calendars")
  public ApiResponse<List<Map<String, Object>>> allCalendars() {
    return ApiResponse.ok(calendars.listAllCalendars());
  }

  @PostMapping("/all-calendars")
  public ApiResponse<Map<String, Object>> saveAllCalendar(@RequestBody Map<String, Object> payload) {
    return ApiResponse.ok(calendars.saveAllCalendar(payload));
  }

  @PutMapping("/all-calendars/{id}")
  public ApiResponse<Map<String, Object>> updateAllCalendar(@PathVariable String id, @RequestBody Map<String, Object> payload) {
    payload.put("id", id);
    return ApiResponse.ok(calendars.saveAllCalendar(payload));
  }

  @PostMapping("/all-calendars/{id}/disable")
  public ApiResponse<Void> disableAllCalendar(@PathVariable long id, @RequestBody(required = false) Map<String, Object> payload) {
    calendars.disableAllCalendar(id, payload == null ? null : number(payload.get("operatorUserId")));
    return ApiResponse.ok();
  }

  @GetMapping("/tags")
  public ApiResponse<List<Map<String, Object>>> tags() {
    return ApiResponse.ok(crud.list("calendar_tag", Set.of("enabled"), Map.of(), "id ASC"));
  }

  @PostMapping("/tags")
  public ApiResponse<Map<String, Object>> createTag(@RequestBody Map<String, Object> payload) {
    var row = crud.create("calendar_tag", TAG_COLUMNS, payload);
    audit.record(number(payload.get("operatorUserId")), "TAG", "CREATE", "calendar_tag", row.get("id"), "标签颜色已新增");
    return ApiResponse.ok(row);
  }

  @PutMapping("/tags/{id}")
  public ApiResponse<Map<String, Object>> updateTag(@PathVariable long id, @RequestBody Map<String, Object> payload) {
    var row = crud.update("calendar_tag", TAG_COLUMNS, id, payload);
    audit.record(number(payload.get("operatorUserId")), "TAG", "UPDATE", "calendar_tag", id, "标签颜色已更新");
    return ApiResponse.ok(row);
  }

  @GetMapping("/settings")
  public ApiResponse<List<Map<String, Object>>> settings() {
    return ApiResponse.ok(jdbc.queryForList("SELECT * FROM system_config ORDER BY config_key"));
  }

  @PutMapping("/settings")
  public ApiResponse<Void> updateSettings(@RequestBody Map<String, String> payload) {
    payload.forEach((key, value) -> jdbc.update("""
        INSERT INTO system_config(config_key, config_value)
        VALUES (?, ?)
        ON CONFLICT(config_key) DO UPDATE SET config_value = EXCLUDED.config_value, updated_at = now()
        """, key, value));
    audit.record(null, "SETTING", "UPDATE", "system_config", "batch", "系统配置已更新");
    return ApiResponse.ok();
  }

  private String number(Object value) {
    return value instanceof String s ? s : null;
  }
}
