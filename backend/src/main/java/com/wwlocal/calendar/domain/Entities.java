package com.wwlocal.calendar.domain;

import java.time.Instant;

public final class Entities {
    private Entities() {
    }

    // ========== User ==========
    public record UserEntity(String id, String name, String email, String mobile, String departmentId,
                             String status, Instant createdAt, Instant updatedAt) {
    }

    // ========== Department ==========
    public record DepartmentEntity(String id, String name, String parentId, Integer sortOrder, Boolean enabled,
                                   Instant createdAt, Instant updatedAt) {
    }

    // ========== Calendar ==========
    public record CalendarEntity(String id, String name, String ownerUserId, String type, String color,
                                 Boolean visible, Instant createdAt, Instant updatedAt) {
    }

    // ========== Organization Calendar ==========
    public record OrganizationCalendarEntity(String id, String name, String scopeType, String color,
                                             String createdBy, Instant createdAt, Instant updatedAt) {
    }

    // ========== Calendar Tag ==========
    public record CalendarTagEntity(Long id, String name, String color, Boolean enabled, Instant createdAt) {
    }

    // ========== Schedule (Event) ==========
    public record ScheduleEntity(String id, String title, String calendarId, Long tagId, String organizerUserId,
                                 Boolean allDay, String timezone, String visibility, String status,
                                 Instant startAt, Instant endAt, String location, String description,
                                 String createdBy, Instant createdAt, Instant updatedAt) {
    }

    // ========== Attachment ==========
    public record AttachmentEntity(String id, String scheduleId, String fileName, String fileUrl, Long fileSize,
                                   String uploadedBy, Instant createdAt, Instant updatedAt) {
    }

    // ========== Export Task ==========
    public record ExportTaskEntity(String id, String taskType, String status, String fileUrl, String requestedBy,
                                   Instant createdAt, Instant updatedAt) {
    }

    // ========== Audit Log ==========
    public record AuditLogEntity(String id, String actorUserId, String action, String targetType, String targetId,
                                 String detail, Instant createdAt) {
    }

    // ========== Backup Restore ==========
    public record BackupRestoreEntity(String id, String operationType, String status, String fileUrl, String createdBy,
                                      Instant createdAt, Instant updatedAt) {
    }

    // ========== System Config ==========
    public record SystemConfigEntity(String id, String configKey, String configValue, String description,
                                     String updatedBy, Instant createdAt, Instant updatedAt) {
    }
}
