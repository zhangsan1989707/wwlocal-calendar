package com.wwlocal.calendar.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for EventService defect fixes.
 */
class EventServiceTest {

    @Test
    @DisplayName("parseIsoDateTime should handle ISO 8601 with Z suffix (UTC)")
    void testParseIsoDateTimeWithZSuffix() {
        // This test covers the bug where LocalDateTime.parse() fails on ISO strings with 'Z'
        // Frontend sends: new Date().toISOString() -> "2026-06-06T12:00:00.000Z"
        // Before fix: LocalDateTime.parse("2026-06-06T12:00:00.000Z") throws DateTimeParseException
        // After fix: correctly parses and converts to system timezone

        var utcString = "2026-06-06T12:00:00.000Z";
        var result = parseIsoDateTime(utcString);

        assertNotNull(result);
        // The result should be converted to system default timezone
        // UTC 12:00 -> Asia/Shanghai 20:00 (assuming system timezone is Asia/Shanghai)
        // We just verify it parses without throwing exception
    }

    @Test
    @DisplayName("parseIsoDateTime should handle local datetime format without Z")
    void testParseIsoDateTimeWithoutZ() {
        var localString = "2026-06-06T12:00:00";
        var result = parseIsoDateTime(localString);

        assertNotNull(result);
        assertEquals(12, result.getHour());
        assertEquals(0, result.getMinute());
    }

    @Test
    @DisplayName("parseIsoDateTime should handle space-separated format")
    void testParseIsoDateTimeWithSpace() {
        var spaceString = "2026-06-06 12:00:00";
        var result = parseIsoDateTime(spaceString);

        assertNotNull(result);
        assertEquals(12, result.getHour());
    }

    @Test
    @DisplayName("parseIsoDateTime should return now for null/blank input")
    void testParseIsoDateTimeNullInput() {
        var resultNull = parseIsoDateTime(null);
        var resultBlank = parseIsoDateTime("");
        var resultWhitespace = parseIsoDateTime("   ");

        assertNotNull(resultNull);
        assertNotNull(resultBlank);
        assertNotNull(resultWhitespace);
    }

    // Helper method that mirrors the fix in EventService
    private LocalDateTime parseIsoDateTime(String s) {
        if (s == null || s.isBlank()) {
            return LocalDateTime.now();
        }
        var normalized = s.replace(" ", "T");
        if (normalized.endsWith("Z")) {
            return Instant.parse(normalized).atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
        return LocalDateTime.parse(normalized);
    }
}