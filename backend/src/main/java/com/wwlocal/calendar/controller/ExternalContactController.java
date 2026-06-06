package com.wwlocal.calendar.controller;

import com.wwlocal.calendar.api.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ExternalContactController {

    private final JdbcTemplate jdbc;

    public ExternalContactController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/external-contacts")
    public ApiResponse<List<Map<String, Object>>> listContacts() {
        var rows = jdbc.queryForList("SELECT * FROM external_contact ORDER BY contact_type, name");
        return ApiResponse.ok(rows);
    }

    @PostMapping("/external-contacts")
    public ApiResponse<Map<String, Object>> createContact(@RequestBody Map<String, Object> payload) {
        var name = payload.get("name");
        if (name == null || String.valueOf(name).isBlank()) {
            throw new IllegalArgumentException("名称不能为空");
        }

        var row = new LinkedHashMap<String, Object>();
        row.put("name", String.valueOf(name));
        row.put("email", payload.getOrDefault("email", ""));
        row.put("company", payload.getOrDefault("company", ""));
        row.put("contact_type", payload.getOrDefault("contact_type", "wechat"));
        row.put("phone", payload.getOrDefault("phone", ""));
        row.put("created_by", payload.getOrDefault("created_by", ""));
        row.put("created_at", Timestamp.from(Instant.now()));

        // 使用简单的 INSERT RETURNING
        var id = jdbc.queryForObject(
            "INSERT INTO external_contact(name, email, company, contact_type, phone, created_by) VALUES (?, ?, ?, ?, ?, ?) RETURNING id",
            Long.class,
            row.get("name"), row.get("email"), row.get("company"),
            row.get("contact_type"), row.get("phone"), row.get("created_by")
        );
        row.put("id", id);

        return ApiResponse.ok(row);
    }

    @DeleteMapping("/external-contacts/{id}")
    public ApiResponse<Void> deleteContact(@PathVariable long id) {
        jdbc.update("DELETE FROM external_contact WHERE id = ?", id);
        return ApiResponse.ok(null);
    }
}