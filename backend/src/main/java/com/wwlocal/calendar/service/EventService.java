package com.wwlocal.calendar.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
        .append(" LEFT JOIN calendars c ON e.calendar_id = c.id")
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
      args.add(userId);
      args.add(userId);
    }

    // By default show only ACTIVE events (not CANCELLED)
    if (!params.containsKey("status")) {
      clauses.add("e.status = 'ACTIVE'");
    } else if (params.get("status") != null && !params.get("status").isBlank()) {
      clauses.add("e.status = ?");
      args.add(params.get("status"));
    }

    // Keyword search on title, location, description, or participant names
    var keyword = params.get("keyword");
    if (keyword != null && !keyword.isBlank()) {
      var pattern = "%" + keyword + "%";
      clauses.add("(e.title ILIKE ? OR e.location ILIKE ? OR e.description ILIKE ? OR EXISTS (SELECT 1 FROM event_participant ep LEFT JOIN users u ON ep.user_id = u.id WHERE ep.event_id = e.id AND u.name ILIKE ?))");
      args.add(pattern);
      args.add(pattern);
      args.add(pattern);
      args.add(pattern);
    }

    if (params.get("calendar_id") != null && !params.get("calendar_id").isBlank()) {
      clauses.add("e.calendar_id = ?");
      args.add(params.get("calendar_id"));
    }
    // Support both 'calendarId' (from frontend CalendarSearch) and 'calendar_id'
    if (params.get("calendarId") != null && !params.get("calendarId").isBlank()) {
      clauses.add("e.calendar_id = ?");
      args.add(params.get("calendarId"));
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
      args.add(params.get("organizer_user_id"));
    }

    if (!clauses.isEmpty()) {
      sql.append(" WHERE ").append(String.join(" AND ", clauses));
    }

    sql.append(" ORDER BY e.start_at LIMIT 500");
    var events = jdbc.queryForList(sql.toString(), args.toArray());

    // 展开重复日程
    return expandRecurrences(events, params);
  }

  /**
   * Parse RRULE and expand recurring events into individual instances.
   * Supports FREQ=DAILY/WEEKLY/MONTHLY/YEARLY with INTERVAL, BYDAY, COUNT, and UNTIL.
   */
  private List<Map<String, Object>> expandRecurrences(List<Map<String, Object>> baseEvents, Map<String, String> params) {
    var result = new ArrayList<Map<String, Object>>();

    // 收集所有有重复规则的 event ID
    var eventIds = new ArrayList<Long>();
    for (var event : baseEvents) {
      eventIds.add(((Number) event.get("id")).longValue());
    }

    if (eventIds.isEmpty()) {
      return result;
    }

    // 查询所有重复规则
    var recurrenceMap = new HashMap<Long, Map<String, Object>>();
    var placeholders = new StringJoiner(",");
    for (var id : eventIds) {
      placeholders.add("?");
    }
    var recurrences = jdbc.queryForList(
        "SELECT * FROM event_recurrence WHERE event_id IN (" + placeholders + ")",
        eventIds.toArray());
    for (var rec : recurrences) {
      recurrenceMap.put(((Number) rec.get("event_id")).longValue(), rec);
    }

    // 查询所有异常
    var exceptions = jdbc.queryForList(
        "SELECT ex.* FROM event_exception ex JOIN event_recurrence er ON ex.recurrence_id = er.id WHERE er.event_id IN (" + placeholders + ")",
        eventIds.toArray());
    var exceptionMap = new HashMap<Long, List<Map<String, Object>>>();
    for (var ex : exceptions) {
      var recurrenceId = ((Number) ex.get("recurrence_id")).longValue();
      exceptionMap.computeIfAbsent(recurrenceId, k -> new ArrayList<>()).add(ex);
    }

    // 确定展开范围：默认前后3个月
    var now = LocalDate.now();
    var expandStart = now.minusMonths(3).atStartOfDay();
    var expandEnd = now.plusMonths(3).plusDays(1).atStartOfDay();

    // 如果传了日期范围参数，使用参数范围
    if (params.get("start_at") != null && !params.get("start_at").isBlank()) {
      expandStart = LocalDateTime.parse(params.get("start_at").replace(" ", "T"));
    }
    if (params.get("start") != null && !params.get("start").isBlank()) {
      expandStart = LocalDateTime.parse(params.get("start").replace(" ", "T"));
    }
    if (params.get("end_at") != null && !params.get("end_at").isBlank()) {
      expandEnd = LocalDateTime.parse(params.get("end_at").replace(" ", "T"));
    }
    if (params.get("end") != null && !params.get("end").isBlank()) {
      expandEnd = LocalDateTime.parse(params.get("end").replace(" ", "T"));
    }

    for (var event : baseEvents) {
      var eventId = ((Number) event.get("id")).longValue();
      var recurrence = recurrenceMap.get(eventId);
      if (recurrence == null) {
        // 非重复日程，直接添加
        result.add(event);
        continue;
      }

      // 解析 RRULE
      var rrule = String.valueOf(recurrence.get("rrule"));
      var rruleEnd = recurrence.get("end_at");
      var occurrenceCount = recurrence.get("occurrence_count");

      var eventStart = toLocalDateTime(event.get("start_at"));
      var eventEnd = toLocalDateTime(event.get("end_at"));
      var duration = java.time.Duration.between(eventStart, eventEnd);

      // 展开重复实例
      var instances = generateRecurrenceInstances(
          eventStart, rrule, rruleEnd, occurrenceCount, expandStart, expandEnd);

      // 处理异常
      var recExceptions = exceptionMap.get(((Number) recurrence.get("id")).longValue());
      var cancelledDates = new java.util.HashSet<LocalDate>();
      var movedDates = new HashMap<LocalDate, LocalDateTime[]>();

      if (recExceptions != null) {
        for (var ex : recExceptions) {
          var origStart = toLocalDateTime(ex.get("original_start_at")).toLocalDate();
          var exType = String.valueOf(ex.get("exception_type"));
          if ("CANCELLED".equals(exType)) {
            cancelledDates.add(origStart);
          } else if ("MOVED".equals(exType)) {
            var newStart = toLocalDateTime(ex.get("new_start_at"));
            var newEnd = toLocalDateTime(ex.get("new_end_at"));
            movedDates.put(origStart, new LocalDateTime[]{newStart, newEnd});
          }
        }
      }

      for (var instanceStart : instances) {
        var instanceDate = instanceStart.toLocalDate();
        if (cancelledDates.contains(instanceDate)) {
          continue; // 跳过已取消的实例
        }

        var instanceEnd = instanceStart.plus(duration);
        if (movedDates.containsKey(instanceDate)) {
          var moved = movedDates.get(instanceDate);
          instanceStart = moved[0];
          instanceEnd = moved[1];
        }

        // 检查是否在展开范围内
        if (instanceEnd.isBefore(expandStart) || instanceStart.isAfter(expandEnd)) {
          continue;
        }

        // 创建展开后的实例（复制原始 event 数据，替换时间）
        var instance = new LinkedHashMap<>(event);
        instance.put("start_at", Timestamp.valueOf(instanceStart));
        instance.put("end_at", Timestamp.valueOf(instanceEnd));
        instance.put("is_recurrence_instance", true);
        instance.put("recurrence_event_id", eventId);
        result.add(instance);
      }
    }

    return result;
  }

  /**
   * Generate all occurrence dates for a recurrence rule within the given range.
   */
  private List<LocalDateTime> generateRecurrenceInstances(
      LocalDateTime start, String rrule, Object endAt, Object count,
      LocalDateTime expandStart, LocalDateTime expandEnd) {

    var instances = new ArrayList<LocalDateTime>();
    var rruleMap = parseRrule(rrule);
    var freq = rruleMap.getOrDefault("FREQ", "DAILY");
    var interval = Integer.parseInt(rruleMap.getOrDefault("INTERVAL", "1"));
    var byDayRaw = rruleMap.get("BYDAY");

    // 解析结束条件
    LocalDateTime ruleEnd = null;
    if (endAt instanceof Timestamp ts) {
      ruleEnd = ts.toLocalDateTime();
    } else if (endAt instanceof java.util.Date d) {
      ruleEnd = d.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
    var maxCount = Integer.MAX_VALUE;
    if (count instanceof Number n) {
      maxCount = n.intValue();
    }

    // 解析 BYDAY
    var byDays = new ArrayList<DayOfWeek>();
    var byDayNums = new ArrayList<Integer>(); // 第N个星期几（每月/每年）
    if (byDayRaw != null) {
      var parts = byDayRaw.split(",");
      for (var part : parts) {
        part = part.trim();
        var numPrefix = 0;
        var dayName = part;
        for (var i = 0; i < part.length(); i++) {
          if (Character.isDigit(part.charAt(i)) || part.charAt(i) == '-') {
            continue;
          }
          var numStr = part.substring(0, i);
          dayName = part.substring(i);
          if (!numStr.isEmpty()) {
            numPrefix = Integer.parseInt(numStr);
          }
          break;
        }
        var dow = parseDayOfWeek(dayName);
        byDays.add(dow);
        byDayNums.add(numPrefix);
      }
    }

    var current = start;
    var countGenerated = 0;
    var totalIterations = 0;
    var maxIterations = 10000; // 安全上限（总迭代次数）

    while (totalIterations < maxIterations) {
      if (countGenerated >= maxCount) {
        break;
      }
      if (current.toLocalDate().isAfter(expandEnd.toLocalDate().plusDays(1))) {
        break;
      }
      if (ruleEnd != null && current.isAfter(ruleEnd)) {
        break;
      }

      if (current.isAfter(expandStart.minusSeconds(1)) || current.isEqual(expandStart)) {
        instances.add(current);
        countGenerated++;
      }

      // 计算下一个实例
      current = nextInstance(current, freq, interval, byDays, byDayNums, start);
      totalIterations++;
    }

    return instances;
  }

  private LocalDateTime nextInstance(LocalDateTime current, String freq, int interval,
      List<DayOfWeek> byDays, List<Integer> byDayNums, LocalDateTime originalStart) {

    if (byDays.isEmpty()) {
      // 简单间隔
      return switch (freq) {
        case "DAILY" -> current.plusDays(interval);
        case "WEEKLY" -> current.plusWeeks(interval);
        case "MONTHLY" -> current.plusMonths(interval);
        case "YEARLY" -> current.plusYears(interval);
        default -> current.plusDays(1);
      };
    }

    if ("WEEKLY".equals(freq)) {
      // 按星期几重复
      var dayOfWeek = current.getDayOfWeek();
      var targetIndex = byDays.indexOf(dayOfWeek);
      if (targetIndex >= 0 && targetIndex < byDays.size() - 1) {
        // 同周内下一个目标星期
        var nextDow = byDays.get(targetIndex + 1);
        var daysUntil = (nextDow.getValue() - dayOfWeek.getValue() + 7) % 7;
        return current.plusDays(daysUntil == 0 ? 7 : daysUntil);
      } else {
        // 下一周的第一个目标星期
        var nextDow = byDays.get(0);
        var daysUntil = (nextDow.getValue() - dayOfWeek.getValue() + 7) % 7;
        if (daysUntil == 0) daysUntil = 7;
        return current.plusDays(daysUntil).plusWeeks(interval - 1);
      }
    }

    if ("MONTHLY".equals(freq)) {
      if (!byDayNums.isEmpty() && byDayNums.get(0) != 0) {
        // 第N个星期几
        var nextMonth = current.plusMonths(interval);
        var targetDow = byDays.get(0);
        var targetNum = byDayNums.get(0);
        if (targetNum > 0) {
          nextMonth = nextMonth.withDayOfMonth(1)
              .with(TemporalAdjusters.dayOfWeekInMonth(targetNum, targetDow));
        } else {
          // 负数表示倒数第N个
          nextMonth = nextMonth.withDayOfMonth(nextMonth.toLocalDate()
              .lengthOfMonth())
              .with(TemporalAdjusters.lastInMonth(targetDow));
        }
        return nextMonth;
      } else {
        // 按日期（每月同一天）
        return current.plusMonths(interval);
      }
    }

    if ("YEARLY".equals(freq)) {
      return current.plusYears(interval);
    }

    return current.plusDays(1);
  }

  private Map<String, String> parseRrule(String rrule) {
    var map = new HashMap<String, String>();
    if (rrule == null || rrule.isBlank()) return map;
    for (var part : rrule.split(";")) {
      var kv = part.split("=", 2);
      if (kv.length == 2) {
        map.put(kv[0].trim().toUpperCase(), kv[1].trim().toUpperCase());
      }
    }
    return map;
  }

  private DayOfWeek parseDayOfWeek(String name) {
    return switch (name.toUpperCase()) {
      case "MO" -> DayOfWeek.MONDAY;
      case "TU" -> DayOfWeek.TUESDAY;
      case "WE" -> DayOfWeek.WEDNESDAY;
      case "TH" -> DayOfWeek.THURSDAY;
      case "FR" -> DayOfWeek.FRIDAY;
      case "SA" -> DayOfWeek.SATURDAY;
      case "SU" -> DayOfWeek.SUNDAY;
      default -> DayOfWeek.MONDAY;
    };
  }

  private LocalDateTime toLocalDateTime(Object value) {
    if (value instanceof Timestamp ts) {
      return ts.toLocalDateTime();
    }
    if (value instanceof java.util.Date d) {
      return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
    if (value instanceof String s) {
      return LocalDateTime.parse(s.replace(" ", "T"));
    }
    return LocalDateTime.now();
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
      
      // 检查权限：只有发起人可以编辑
      var operatorUserId = payload.get("operatorUserId");
      if (operatorUserId != null) {
        var existingEvent = jdbc.queryForList("SELECT organizer_user_id FROM event WHERE id = ?", id);
        if (!existingEvent.isEmpty()) {
          var organizerId = existingEvent.get(0).get("organizer_user_id");
          if (!String.valueOf(operatorUserId).equals(String.valueOf(organizerId))) {
            throw new SecurityException("只有发起人可以编辑此日程");
          }
        }
      }
      
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
    var participantIds = (List<String>) payload.get("participantIds");
    if (participantIds == null || participantIds.isEmpty()) {
      return;
    }
    for (var pid : participantIds) {
      jdbc.update(
          "INSERT INTO event_participant(event_id, user_id, response_status) VALUES (?, ?, 'NEEDS_ACTION')",
          eventId, String.valueOf(pid)
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
          assigneeUserId != null ? String.valueOf(assigneeUserId) : null,
          priority, completed
      );
    }
  }

  /**
   * Upsert the recurrence rule for the event.
   * If rrule is null/blank, delete any existing recurrence.
   */
  private void replaceRecurrence(long eventId, Map<String, Object> payload) {
    String rrule = (String) payload.getOrDefault("rrule", payload.get("recurrence_rule"));
    if (rrule == null || rrule.isBlank()) {
      jdbc.update("DELETE FROM event_recurrence WHERE event_id = ?", eventId);
      return;
    }
    var existing = jdbc.queryForList(
        "SELECT id FROM event_recurrence WHERE event_id = ?", eventId);
    if (existing.isEmpty()) {
      jdbc.update(
          "INSERT INTO event_recurrence(event_id, rrule, end_at, occurrence_count) VALUES (?, ?, ?::timestamptz, ?)",
          eventId, rrule, payload.get("recurrence_end"), payload.get("occurrence_count"));
    } else {
      jdbc.update(
          "UPDATE event_recurrence SET rrule = ?, end_at = ?::timestamptz, occurrence_count = ? WHERE event_id = ?",
          rrule, payload.get("recurrence_end"), payload.get("occurrence_count"), eventId);
    }
  }

  /**
   * Remove (cancel) an event. 
   * scope=single: soft-delete single event (status = CANCELLED).
   * scope=series: cancel the entire recurrence series.
   */
  public void remove(long id, String operatorUserId, String scope) {
    if ("series".equals(scope)) {
      // 删除整个重复系列：取消主事件
      if (operatorUserId != null) {
        var event = jdbc.queryForList("SELECT organizer_user_id FROM event WHERE id = ?", id);
        if (!event.isEmpty()) {
          var organizerId = event.get(0).get("organizer_user_id");
          if (!String.valueOf(operatorUserId).equals(String.valueOf(organizerId))) {
            throw new SecurityException("只有发起人可以删除此日程");
          }
        }
      }
      jdbc.update("DELETE FROM event_recurrence WHERE event_id = ?", id);
      jdbc.update("UPDATE event SET status = 'CANCELLED', updated_at = now() WHERE id = ?", id);
      if (operatorUserId != null) {
        audit.record(operatorUserId, "event", "cancel_series", "event", id, "整个重复系列已取消");
      }
      return;
    }

    if (!"single".equals(scope)) {
      throw new UnsupportedOperationException("series delete not implemented");
    }
    
    // 检查权限：只有发起人可以删除
    if (operatorUserId != null) {
      var event = jdbc.queryForList("SELECT organizer_user_id FROM event WHERE id = ?", id);
      if (!event.isEmpty()) {
        var organizerId = event.get(0).get("organizer_user_id");
        if (!String.valueOf(operatorUserId).equals(String.valueOf(organizerId))) {
          throw new SecurityException("只有发起人可以删除此日程");
        }
      }
    }
    
    jdbc.update("UPDATE event SET status = 'CANCELLED', updated_at = now() WHERE id = ?", id);
    if (operatorUserId != null) {
      audit.record(operatorUserId, "event", "cancel", "event", id, "单次取消事件");
    }
  }

  /**
   * Respond to an event invitation. Upserts a row in event_participant.
   */
  public void respond(long id, String userId, String status) {
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
    var userIds = (List<String>) payload.get("user_ids");
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
    return jdbc.queryForList(sql, userIds.toArray(new String[0]), end, start);
  }

  /**
   * Export events to an Excel-like structure and record an export_task.
   */
  public Map<String, Object> exportEvents(String userId, String scope) {
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
  public Path exportEvents(Path outputDir, String userId) throws Exception {
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

  public List<Map<String, Object>> getEventAttachments(long eventId) {
    return jdbc.queryForList("SELECT * FROM event_attachment WHERE event_id = ?", eventId);
  }

  public List<Map<String, Object>> getEventParticipants(long eventId) {
    return jdbc.queryForList("SELECT ep.*, u.name FROM event_participant ep LEFT JOIN users u ON ep.user_id = u.id WHERE ep.event_id = ?", eventId);
  }

  public List<Map<String, Object>> getEventReminders(long eventId) {
    return jdbc.queryForList("SELECT * FROM event_reminder WHERE event_id = ?", eventId);
  }

  public List<Map<String, Object>> getEventTodos(long eventId) {
    return jdbc.queryForList("SELECT * FROM event_todo WHERE event_id = ?", eventId);
  }

  public Map<String, Object> toggleTodo(long todoId, boolean completed, String operatorUserId) {
    jdbc.update("UPDATE event_todo SET completed = ?, updated_at = now() WHERE id = ?", completed, todoId);
    if (operatorUserId != null) {
      audit.record(operatorUserId, "todo", completed ? "complete" : "reopen", "event_todo", todoId, completed ? "待办已完成" : "待办已重新打开");
    }
    return jdbc.queryForMap("SELECT * FROM event_todo WHERE id = ?", todoId);
  }
}
