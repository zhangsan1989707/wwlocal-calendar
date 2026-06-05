package com.wwlocal.calendar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public final class Requests {
    private Requests() {
    }

    // ========== User ==========
    public record UserRequest(@NotBlank String name, String email, String mobile, String departmentId,
                              @NotBlank String status) {
    }

    // ========== Department ==========
    public record DepartmentRequest(@NotBlank String name, String parentId, Integer sortOrder, Boolean enabled) {
    }

    // ========== Calendar ==========
    public record CalendarRequest(@NotBlank String name, String ownerUserId, @NotBlank String type, String color,
                                  Boolean visible) {
    }

    // ========== Organization Calendar ==========
    public record OrganizationCalendarRequest(@NotBlank String name, @NotBlank String scopeType, String color,
                                              String createdBy) {
    }

    // ========== Schedule (Event) ==========
    public record ScheduleRequest(@NotBlank String title, @NotBlank String calendarId, Long tagId,
                                  String organizerUserId, Boolean allDay, String timezone, String visibility,
                                  String status, @NotNull Instant startAt, @NotNull Instant endAt,
                                  String location, String description, String createdBy) {
    }

    // ========== Calendar Tag ==========
    public record CalendarTagRequest(@NotBlank String name, @NotBlank String color, Boolean enabled) {
    }

    // ========== Attachment ==========
    public record AttachmentRequest(@NotBlank String scheduleId, @NotBlank String fileName, @NotBlank String fileUrl,
                                    Long fileSize, String uploadedBy) {
    }

    // ========== Export Task ==========
    public record ExportTaskRequest(@NotBlank String taskType, @NotBlank String status, String fileUrl,
                                    String requestedBy) {
    }

    // ========== Audit Log ==========
    public record AuditLogRequest(String actorUserId, @NotBlank String action, @NotBlank String targetType,
                                  String targetId, String detail) {
    }

    // ========== Backup Restore ==========
    public record BackupRestoreRequest(@NotBlank String operationType, @NotBlank String status, String fileUrl,
                                       String createdBy) {
    }

    // ========== System Config ==========
    public record SystemConfigRequest(@NotBlank String configKey, @NotBlank String configValue, String description,
                                      String updatedBy) {
    }
}
