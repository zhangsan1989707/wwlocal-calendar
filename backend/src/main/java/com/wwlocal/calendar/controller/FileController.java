package com.wwlocal.calendar.controller;

import com.wwlocal.calendar.api.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class FileController {

    private final JdbcTemplate jdbc;
    private final Path uploadDir;

    public FileController(JdbcTemplate jdbc,
                          @Value("${app.upload-dir:../uploads}") String uploadDirPath) {
        this.jdbc = jdbc;
        this.uploadDir = Paths.get(uploadDirPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("无法创建上传目录: " + this.uploadDir, e);
        }
    }

    @PostMapping("/files/upload")
    public ApiResponse<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "eventId", required = false) Long eventId,
            @RequestParam("userId") String userId) throws IOException {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        // 生成唯一文件名
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }
        String storedName = UUID.randomUUID().toString().replace("-", "") + ext;

        // 按年月创建子目录
        String yearMonth = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM"));
        Path targetDir = this.uploadDir.resolve(yearMonth);
        Files.createDirectories(targetDir);

        Path targetPath = targetDir.resolve(storedName);
        file.transferTo(targetPath.toFile());

        // 插入 event_attachment 记录
        String filePath = targetPath.toString();
        long fileSize = file.getSize();
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO event_attachment(event_id, uploaded_by, file_name, file_path, file_size, content_type) VALUES (?, ?, ?, ?, ?, ?) RETURNING id",
                new String[]{"id"});
            ps.setObject(1, eventId);
            ps.setString(2, userId);
            ps.setString(3, originalName != null ? originalName : "file");
            ps.setString(4, filePath);
            ps.setLong(5, fileSize);
            ps.setString(6, contentType);
            return ps;
        }, keyHolder);

        long attachmentId = keyHolder.getKey().longValue();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", attachmentId);
        result.put("event_id", eventId);
        result.put("file_name", originalName);
        result.put("file_path", filePath);
        result.put("file_size", fileSize);
        result.put("content_type", contentType);
        result.put("uploaded_by", userId);
        result.put("created_at", Timestamp.from(Instant.now()));

        return ApiResponse.ok(result);
    }

    @GetMapping("/files/{attachmentId}/download")
    public void downloadFile(@PathVariable long attachmentId, jakarta.servlet.http.HttpServletResponse response) throws IOException {
        var rows = jdbc.queryForList("SELECT * FROM event_attachment WHERE id = ?", attachmentId);
        if (rows.isEmpty()) {
            response.sendError(404, "附件不存在");
            return;
        }
        var attachment = rows.get(0);
        String filePath = String.valueOf(attachment.get("file_path"));
        String fileName = String.valueOf(attachment.get("file_name"));
        String contentType = String.valueOf(attachment.get("content_type"));

        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            response.sendError(404, "文件不存在");
            return;
        }

        response.setContentType(contentType);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + java.net.URLEncoder.encode(fileName, "UTF-8") + "\"");
        response.setContentLengthLong(Files.size(path));

        try (FileInputStream fis = new FileInputStream(path.toFile())) {
            FileCopyUtils.copy(fis, response.getOutputStream());
        }
    }
}