package com.wwlocal.calendar.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class EventServiceTest {

  private final EventService service = new EventService(null, null, null);

  // ---- nextInstance weekly BYDAY tests ----

  /**
   * 场景：FREQ=WEEKLY;BYDAY=MO,WE,FR；当前日期 = 周二（不在 BYDAY 列表中）
   * 期望：下一个实例是本周三（WE），而非跳到下周一
   */
  @Test
  void nextInstance_weekly_byday_currentNotInList_returnsNextInSameWeek() {
    // 2026-06-02 is a Tuesday
    var current = LocalDateTime.of(2026, 6, 2, 9, 0);
    var byDays = List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
    var byDayNums = List.of(0, 0, 0);

    var result = service.nextInstance(current, "WEEKLY", 1, byDays, byDayNums, current);

    // Expected: Wednesday 2026-06-03 (next BYDAY in same week)
    assertEquals(LocalDateTime.of(2026, 6, 3, 9, 0), result);
  }

  /**
   * 场景：FREQ=WEEKLY;BYDAY=MO,WE,FR；当前日期 = 周五（本周最后一个 BYDAY）
   * 期望：下一周的第一个 BYDAY（周一）
   */
  @Test
  void nextInstance_weekly_byday_currentIsLastInWeek_returnsFirstOfNextWeek() {
    // 2026-06-05 is a Friday
    var current = LocalDateTime.of(2026, 6, 5, 9, 0);
    var byDays = List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
    var byDayNums = List.of(0, 0, 0);

    var result = service.nextInstance(current, "WEEKLY", 1, byDays, byDayNums, current);

    // Expected: Monday 2026-06-08 (first BYDAY of next week)
    assertEquals(LocalDateTime.of(2026, 6, 8, 9, 0), result);
  }

  /**
   * 场景：FREQ=WEEKLY;BYDAY=MO,WE,FR；当前日期 = 周三（在 BYDAY 列表中，但不是最后一个）
   * 期望：本周五
   */
  @Test
  void nextInstance_weekly_byday_currentInList_movesToNextInSameWeek() {
    // 2026-06-03 is a Wednesday
    var current = LocalDateTime.of(2026, 6, 3, 9, 0);
    var byDays = List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
    var byDayNums = List.of(0, 0, 0);

    var result = service.nextInstance(current, "WEEKLY", 1, byDays, byDayNums, current);

    // Expected: Friday 2026-06-05
    assertEquals(LocalDateTime.of(2026, 6, 5, 9, 0), result);
  }

  /**
   * 场景：FREQ=WEEKLY;BYDAY=MO,WE,FR；当前日期 = 周六（不在 BYDAY 列表中，且本周已无后续 BYDAY）
   * 期望：下周一
   */
  @Test
  void nextInstance_weekly_byday_currentIsSaturday_returnsNextMonday() {
    // 2026-06-06 is a Saturday
    var current = LocalDateTime.of(2026, 6, 6, 9, 0);
    var byDays = List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
    var byDayNums = List.of(0, 0, 0);

    var result = service.nextInstance(current, "WEEKLY", 1, byDays, byDayNums, current);

    // Expected: Monday 2026-06-08
    assertEquals(LocalDateTime.of(2026, 6, 8, 9, 0), result);
  }

  /**
   * 场景：FREQ=WEEKLY;BYDAY=MO（仅一个 BYDAY）；当前日期 = 周一
   * 期望：下周一
   */
  @Test
  void nextInstance_weekly_byday_singleDay_returnsNextWeek() {
    var current = LocalDateTime.of(2026, 6, 1, 9, 0); // Monday
    var byDays = List.of(DayOfWeek.MONDAY);
    var byDayNums = List.of(0);

    var result = service.nextInstance(current, "WEEKLY", 1, byDays, byDayNums, current);

    // Expected: Monday 2026-06-08
    assertEquals(LocalDateTime.of(2026, 6, 8, 9, 0), result);
  }

  // ---- generateRecurrenceInstances integration tests ----

  /**
   * 场景：从周二开始的 WEEKLY BYDAY=MO,WE,FR 重复规则
   * 期望：生成的实例包含本周三和周五，不会跳过它们
   */
  @Test
  void generateInstances_weekly_byday_startOnTuesday_includesWednesdayAndFriday() {
    var start = LocalDateTime.of(2026, 6, 2, 9, 0); // Tuesday
    // Expand range: cover the whole week
    var expandStart = LocalDateTime.of(2026, 6, 1, 0, 0);
    var expandEnd = LocalDateTime.of(2026, 6, 14, 0, 0);

    var instances = service.generateRecurrenceInstances(
        start, "FREQ=WEEKLY;BYDAY=MO,WE,FR", null, null, expandStart, expandEnd);

    // Should include: Tue 6/2, Wed 6/3, Fri 6/5, Mon 6/8, Wed 6/10, Fri 6/12
    assertEquals(6, instances.size());
    assertEquals(LocalDateTime.of(2026, 6, 2, 9, 0), instances.get(0)); // original start
    assertEquals(LocalDateTime.of(2026, 6, 3, 9, 0), instances.get(1)); // Wednesday
    assertEquals(LocalDateTime.of(2026, 6, 5, 9, 0), instances.get(2)); // Friday
    assertEquals(LocalDateTime.of(2026, 6, 8, 9, 0), instances.get(3)); // Monday
    assertEquals(LocalDateTime.of(2026, 6, 10, 9, 0), instances.get(4)); // Wednesday
    assertEquals(LocalDateTime.of(2026, 6, 12, 9, 0), instances.get(5)); // Friday
  }

  /**
   * 场景：从周一开始的 WEEKLY BYDAY=MO,WE,FR（正常情况，BYDAY 匹配）
   * 期望：回归测试，确保修复不破坏已有正确行为
   */
  @Test
  void generateInstances_weekly_byday_startOnMonday_normalCase() {
    var start = LocalDateTime.of(2026, 6, 1, 9, 0); // Monday
    var expandStart = LocalDateTime.of(2026, 6, 1, 0, 0);
    var expandEnd = LocalDateTime.of(2026, 6, 14, 0, 0);

    var instances = service.generateRecurrenceInstances(
        start, "FREQ=WEEKLY;BYDAY=MO,WE,FR", null, null, expandStart, expandEnd);

    // Should include: Mon 6/1, Wed 6/3, Fri 6/5, Mon 6/8, Wed 6/10, Fri 6/12
    assertEquals(6, instances.size());
    assertEquals(LocalDateTime.of(2026, 6, 1, 9, 0), instances.get(0));
    assertEquals(LocalDateTime.of(2026, 6, 3, 9, 0), instances.get(1));
    assertEquals(LocalDateTime.of(2026, 6, 5, 9, 0), instances.get(2));
    assertEquals(LocalDateTime.of(2026, 6, 8, 9, 0), instances.get(3));
    assertEquals(LocalDateTime.of(2026, 6, 10, 9, 0), instances.get(4));
    assertEquals(LocalDateTime.of(2026, 6, 12, 9, 0), instances.get(5));
  }

  /**
   * 场景：DAILY 重复（无 BYDAY），确保不受影响
   */
  @Test
  void generateInstances_daily_worksCorrectly() {
    var start = LocalDateTime.of(2026, 6, 1, 9, 0);
    var expandStart = LocalDateTime.of(2026, 6, 1, 0, 0);
    var expandEnd = LocalDateTime.of(2026, 6, 5, 0, 0);

    var instances = service.generateRecurrenceInstances(
        start, "FREQ=DAILY", null, null, expandStart, expandEnd);

    assertEquals(5, instances.size());
    assertEquals(LocalDateTime.of(2026, 6, 1, 9, 0), instances.get(0));
    assertEquals(LocalDateTime.of(2026, 6, 5, 9, 0), instances.get(4));
  }
}