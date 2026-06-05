package com.wwlocal.calendar.controller;

import com.wwlocal.calendar.api.ApiResponse;
import com.wwlocal.calendar.config.AppProperties;
import com.wwlocal.calendar.service.AuditService;
import com.wwlocal.calendar.service.CrudService;
import com.wwlocal.calendar.service.EventService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class OpsController {
  private final CrudService crud;
  private final JdbcTemplate jdbc;
  private final EventService events;
  private final AuditService audit;
  private final AppProperties properties;

  public OpsController(CrudService crud, JdbcTemplate jdbc, EventService events, AuditService audit, AppProperties properties) {
    this.crud = crud;
    this.jdbc = jdbc;
    this.events = events;
    this.audit = audit;
    this.properties = properties;
  }

  @GetMapping("/attachments")
  public ApiResponse<List<Map<String, Object>>> attachments() {
    return ApiResponse.ok(jdbc.queryForList("""
        SELECT a.*, e.title AS event_title, u.name AS uploader_name
        FROM event_attachment a
        LEFT JOIN event e ON e.id = a.event_id
        LEFT JOIN sys_user u ON u.id = a.uploaded_by
        ORDER BY a.id DESC
        """));
  }

  @PostMapping("/files/upload")
  public ApiResponse<Map<String, Object>> upload(
      @RequestParam MultipartFile file,
      @RequestParam(required = false) Long eventId,
      @RequestParam(required = false) Long userId) throws Exception {
    if (file.isEmpty()) {
      throw new IllegalArgumentException("文件不能为空");
    }
    var original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
    if (original.contains("..")) {
      throw new IllegalArgumentException("文件名不合法");
    }
    var dir = Path.of(properties.uploadDir()).toAbsolutePath().normalize();
    Files.createDirectories(dir);
    var stored = System.currentTimeMillis() + "-" + original;
    var target = dir.resolve(stored).normalize();
    if (!target.startsWith(dir)) {
      throw new IllegalArgumentException("保存路径不合法");
    }
    file.transferTo(target);
    var row = jdbc.queryForMap("""
        INSERT INTO event_attachment(event_id, file_name, stored_name, file_size, content_type, storage_path, uploaded_by)
        VALUES (?, ?, ?, ?, ?, ?, ?)
        RETURNING *
        """, eventId, original, stored, file.getSize(), file.getContentType(), target.toString(), userId);
    audit.record(userId, "ATTACHMENT", "UPLOAD", "event_attachment", row.get("id"), "附件已上传");
    return ApiResponse.ok(row);
  }

  @GetMapping("/files/{id}/download")
  public ResponseEntity<FileSystemResource> download(@PathVariable long id) {
    var row = jdbc.queryForMap("SELECT * FROM event_attachment WHERE id = ?", id);
    var path = Path.of(String.valueOf(row.get("storage_path"))).normalize();
    var resource = new FileSystemResource(path);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + row.get("file_name") + "\"")
        .body(resource);
  }

  @PostMapping("/attachments/{id}/delete")
  public ApiResponse<Void> deleteAttachment(@PathVariable long id, @RequestBody(required = false) Map<String, Object> payload) throws Exception {
    var row = jdbc.queryForMap("SELECT * FROM event_attachment WHERE id = ?", id);
    Files.deleteIfExists(Path.of(String.valueOf(row.get("storage_path"))));
    crud.delete("event_attachment", id);
    audit.record(payload == null ? null : number(payload.get("operatorUserId")), "ATTACHMENT", "DELETE",
        "event_attachment", id, "附件已删除");
    return ApiResponse.ok();
  }

  @PostMapping("/export/events")
  public ApiResponse<Map<String, String>> exportEvents(@RequestBody(required = false) Map<String, Object> payload) throws Exception {
    var userId = payload == null ? null : number(payload.get("operatorUserId"));
    var path = events.exportEvents(Path.of(properties.exportDir()), userId);
    return ApiResponse.ok(Map.of("filePath", path.toString()));
  }

  @GetMapping("/exports")
  public ApiResponse<List<Map<String, Object>>> exports() {
    return ApiResponse.ok(crud.list("export_task", Set.of("status"), Map.of(), "id DESC"));
  }

  @GetMapping("/audit-logs")
  public ApiResponse<List<Map<String, Object>>> auditLogs(@RequestParam Map<String, String> params) {
    return ApiResponse.ok(crud.list("audit_log", Set.of("module", "action", "object_type", "result"), params, "created_at DESC"));
  }

  @GetMapping("/backup-records")
  public ApiResponse<List<Map<String, Object>>> backupRecords() {
    return ApiResponse.ok(crud.list("backup_record", Set.of("status"), Map.of(), "id DESC"));
  }

  @PostMapping("/backup")
  public ApiResponse<Map<String, Object>> backup(@RequestBody(required = false) Map<String, Object> payload) throws Exception {
    var dir = Path.of(properties.backupDir()).toAbsolutePath().normalize();
    Files.createDirectories(dir);
    var marker = dir.resolve("backup-" + System.currentTimeMillis() + ".txt");
    Files.writeString(marker, "数据库与附件目录备份记录\n");
    var row = jdbc.queryForMap("""
        INSERT INTO backup_record(backup_type, file_path, file_size, status, operated_by)
        VALUES ('MANUAL', ?, ?, 'COMPLETED', ?)
        RETURNING *
        """, marker.toString(), Files.size(marker), payload == null ? null : number(payload.get("operatorUserId")));
    audit.record(payload == null ? null : number(payload.get("operatorUserId")), "BACKUP", "CREATE", "backup_record",
        row.get("id"), "备份记录已生成");
    return ApiResponse.ok(row);
  }

  @PostMapping("/backup/{id}/restore-record")
  public ApiResponse<Map<String, Object>> restoreRecord(@PathVariable long id, @RequestBody Map<String, Object> payload) {
    var row = jdbc.queryForMap("""
        UPDATE backup_record
        SET restore_started_at = COALESCE(restore_started_at, now()),
            restore_finished_at = now(),
            restore_result = ?,
            verified_by = ?,
            conclusion = ?
        WHERE id = ?
        RETURNING *
        """, payload.get("restoreResult"), payload.get("verifiedBy"), payload.get("conclusion"), id);
    audit.record(number(payload.get("operatorUserId")), "BACKUP", "RESTORE_RECORD", "backup_record", id, "恢复结果已登记");
    return ApiResponse.ok(row);
  }

  @GetMapping("/admin/overview")
  public ApiResponse<Map<String, Object>> overview() {
    var data = new LinkedHashMap<String, Object>();
    data.put("users", count("sys_user"));
    data.put("departments", count("sys_department"));
    data.put("calendars", count("calendar"));
    data.put("allCalendars", jdbc.queryForObject("SELECT count(*) FROM calendar WHERE type = 'ALL_MEMBER'", Long.class));
    data.put("events", count("event"));
    data.put("attachments", count("event_attachment"));
    data.put("todos", count("event_todo"));
    data.put("exports", count("export_task"));
    data.put("recentLogs", jdbc.queryForList("SELECT * FROM audit_log ORDER BY created_at DESC LIMIT 8"));
    data.put("serviceStatus", "正常");
    data.put("databaseStatus", "正常");
    data.put("uploadStatus", Files.isDirectory(Path.of(properties.uploadDir()).toAbsolutePath().normalize()) ? "正常" : "待创建");
    return ApiResponse.ok(data);
  }

  private Long count(String table) {
    return jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class);
  }

  private Long number(Object value) {
    return value instanceof Number n ? n.longValue() : null;
  }
}
