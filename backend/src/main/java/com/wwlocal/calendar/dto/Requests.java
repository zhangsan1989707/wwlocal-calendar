package com.wwlocal.calendar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public final class Requests {
    private Requests() {
    }

    public record UserRequest(@NotBlank String name, String email, String mobile, String departmentId,
                              @NotBlank String status) {
    }

    public record DepartmentRequest(@NotBlank String name, String parentId, Integer sortOrder) {
    }

    public record CalendarRequest(@NotBlank String name, String ownerUserId, @NotBlank String type, String color,
                                  Boolean visible) {
    }

    public record OrganizationCalendarRequest(@NotBlank String name, @NotBlank String scopeType, String color,
                                              String createdBy) {
    }

    public record ScheduleRequest(@NotBlank String title, @NotBlank String calendarId, @NotNull Instant startAt,
                                  @NotNull Instant endAt, String location, String description, String createdBy) {
    }

    public record AttachmentRequest(@NotBlank String scheduleId, @NotBlank String fileName, @NotBlank String fileUrl,
                                    Long fileSize, String uploadedBy) {
    }

    public record ExportTaskRequest(@NotBlank String taskType, @NotBlank String status, String fileUrl,
                                    String requestedBy) {
    }

    public record AuditLogRequest(String actorUserId, @NotBlank String action, @NotBlank String targetType,
                                  String targetId, String detail) {
    }

    public record BackupRestoreRequest(@NotBlank String operationType, @NotBlank String status, String fileUrl,
                                       String createdBy) {
    }

    public record SystemConfigRequest(@NotBlank String configKey, @NotBlank String configValue, String description,
                                      String updatedBy) {
    }
}
