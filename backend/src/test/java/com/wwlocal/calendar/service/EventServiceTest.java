package com.wwlocal.calendar.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for EventService recurrence logic, specifically for negative BYDAY values
 * in MONTHLY frequency (e.g., -1FR for last Friday, -2MO for second-to-last Monday).
 */
class EventServiceTest {

    private EventService eventService;

    @BeforeEach
    void setUp() {
        eventService = new EventService(null, null, null);
    }

    /**
     * Test case: MONTHLY frequency with BYDAY=-1FR (last Friday of each month)
     * Expected: Should generate instances on the last Friday of each month
     */
    @Test
    void testMonthlyLastFriday() {
        // Start from January 1, 2026
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 10, 0);
        String rrule = "FREQ=MONTHLY;BYDAY=-1FR";
        LocalDateTime expandStart = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime expandEnd = LocalDateTime.of(2026, 4, 30, 23, 59);

        var instances = eventService.generateRecurrenceInstances(
            start, rrule, null, null, expandStart, expandEnd);

        // Expected: Jan 30 (last Friday), Feb 27 (last Friday), Mar 27 (last Friday)
        assertEquals(3, instances.size(), "Should generate 3 instances for Jan-Mar 2026");
        
        // Verify January 30, 2026 is the last Friday
        assertEquals(LocalDateTime.of(2026, 1, 30, 10, 0), instances.get(0));
        assertTrue(instances.get(0).getDayOfWeek() == DayOfWeek.FRIDAY);
        assertTrue(instances.get(0).getDayOfMonth() >= 25); // Last Friday should be in last week
        
        // Verify February 27, 2026 is the last Friday
        assertEquals(LocalDateTime.of(2026, 2, 27, 10, 0), instances.get(1));
        
        // Verify March 27, 2026 is the last Friday
        assertEquals(LocalDateTime.of(2026, 3, 27, 10, 0), instances.get(2));
    }

    /**
     * Test case: MONTHLY frequency with BYDAY=-2FR (second-to-last Friday of each month)
     * Expected: Should generate instances on the second-to-last Friday, NOT the last Friday
     */
    @Test
    void testMonthlySecondToLastFriday() {
        // Start from January 1, 2026
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 10, 0);
        String rrule = "FREQ=MONTHLY;BYDAY=-2FR";
        LocalDateTime expandStart = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime expandEnd = LocalDateTime.of(2026, 4, 30, 23, 59);

        var instances = eventService.generateRecurrenceInstances(
            start, rrule, null, null, expandStart, expandEnd);

        // Expected: Jan 23 (2nd-to-last Friday), Feb 20 (2nd-to-last Friday), Mar 20 (2nd-to-last Friday)
        assertEquals(3, instances.size(), "Should generate 3 instances for Jan-Mar 2026");
        
        // January 2026: Fridays are 2, 9, 16, 23, 30
        // -2FR should be the 23rd (NOT the 30th which is -1FR)
        assertEquals(LocalDateTime.of(2026, 1, 23, 10, 0), instances.get(0), 
            "January -2FR should be the 23rd, not the 30th (last Friday)");
        
        // February 2026: Fridays are 6, 13, 20, 27
        // -2FR should be the 20th (NOT the 27th which is -1FR)
        assertEquals(LocalDateTime.of(2026, 2, 20, 10, 0), instances.get(1),
            "February -2FR should be the 20th, not the 27th (last Friday)");
        
        // March 2026: Fridays are 6, 13, 20, 27
        // -2FR should be the 20th (NOT the 27th which is -1FR)
        assertEquals(LocalDateTime.of(2026, 3, 20, 10, 0), instances.get(2),
            "March -2FR should be the 20th, not the 27th (last Friday)");
    }

    /**
     * Test case: MONTHLY frequency with BYDAY=-3MO (third-to-last Monday of each month)
     * Expected: Should generate instances on the third-to-last Monday
     */
    @Test
    void testMonthlyThirdToLastMonday() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 10, 0);
        String rrule = "FREQ=MONTHLY;BYDAY=-3MO";
        LocalDateTime expandStart = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime expandEnd = LocalDateTime.of(2026, 2, 28, 23, 59);

        var instances = eventService.generateRecurrenceInstances(
            start, rrule, null, null, expandStart, expandEnd);

        assertEquals(2, instances.size(), "Should generate 2 instances for Jan-Feb 2026");
        
        // January 2026: Mondays are 5, 12, 19, 26
        // -3MO should be the 12th (NOT the 26th which is -1MO)
        assertEquals(LocalDateTime.of(2026, 1, 12, 10, 0), instances.get(0),
            "January -3MO should be the 12th");
        
        // February 2026: Mondays are 2, 9, 16, 23
        // -3MO should be the 9th (NOT the 23rd which is -1MO)
        assertEquals(LocalDateTime.of(2026, 2, 9, 10, 0), instances.get(1),
            "February -3MO should be the 9th");
    }

    /**
     * Test case: MONTHLY frequency with positive BYDAY=2MO (second Monday of each month)
     * Expected: Should still work correctly for positive values
     */
    @Test
    void testMonthlySecondMonday() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 10, 0);
        String rrule = "FREQ=MONTHLY;BYDAY=2MO";
        LocalDateTime expandStart = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime expandEnd = LocalDateTime.of(2026, 3, 31, 23, 59);

        var instances = eventService.generateRecurrenceInstances(
            start, rrule, null, null, expandStart, expandEnd);

        assertEquals(3, instances.size(), "Should generate 3 instances for Jan-Mar 2026");
        
        // January 2026: Mondays are 5, 12, 19, 26
        // 2MO should be the 12th
        assertEquals(LocalDateTime.of(2026, 1, 12, 10, 0), instances.get(0));
        
        // February 2026: Mondays are 2, 9, 16, 23
        // 2MO should be the 9th
        assertEquals(LocalDateTime.of(2026, 2, 9, 10, 0), instances.get(1));
        
        // March 2026: Mondays are 2, 9, 16, 23, 30
        // 2MO should be the 9th
        assertEquals(LocalDateTime.of(2026, 3, 9, 10, 0), instances.get(2));
    }

    /**
     * Test case: MONTHLY frequency with BYDAY=1MO (first Monday of each month)
     * Expected: Should work correctly for the first occurrence
     */
    @Test
    void testMonthlyFirstMonday() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 10, 0);
        String rrule = "FREQ=MONTHLY;BYDAY=1MO";
        LocalDateTime expandStart = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime expandEnd = LocalDateTime.of(2026, 2, 28, 23, 59);

        var instances = eventService.generateRecurrenceInstances(
            start, rrule, null, null, expandStart, expandEnd);

        assertEquals(2, instances.size(), "Should generate 2 instances for Jan-Feb 2026");
        
        // January 2026: First Monday is the 5th
        assertEquals(LocalDateTime.of(2026, 1, 5, 10, 0), instances.get(0));
        
        // February 2026: First Monday is the 2nd
        assertEquals(LocalDateTime.of(2026, 2, 2, 10, 0), instances.get(1));
    }
}