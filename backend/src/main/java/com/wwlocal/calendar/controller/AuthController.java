package com.wwlocal.calendar.controller;

import com.wwlocal.calendar.api.ApiResponse;
import com.wwlocal.calendar.security.JwtUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JdbcTemplate jdbc;
    private final JwtUtil jwtUtil;

    public AuthController(JdbcTemplate jdbc, JwtUtil jwtUtil) {
        this.jdbc = jdbc;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        var username = body.get("username");
        var password = body.get("password");

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return ApiResponse.error(400, "用户名和密码不能为空");
        }

        var rows = jdbc.queryForList("SELECT * FROM app_user WHERE username = ? AND enabled = true", username);
        if (rows.isEmpty()) {
            return ApiResponse.error(401, "用户名或密码错误");
        }

        var user = rows.get(0);
        var hash = sha256(password);
        if (!hash.equals(String.valueOf(user.get("password_hash")))) {
            return ApiResponse.error(401, "用户名或密码错误");
        }

        var userId = resolveBusinessUserId(username, String.valueOf(user.get("id")));
        var role = String.valueOf(user.get("role"));
        var token = jwtUtil.generateToken(userId, username, role);

        var result = new LinkedHashMap<String, Object>();
        result.put("token", token);
        result.put("userId", userId);
        result.put("username", username);
        result.put("displayName", user.get("display_name"));
        result.put("role", role);

        return ApiResponse.ok(result);
    }

    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        var username = body.get("username");
        var password = body.get("password");
        var displayName = body.getOrDefault("displayName", username);

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return ApiResponse.error(400, "用户名和密码不能为空");
        }

        var existing = jdbc.queryForList("SELECT id FROM app_user WHERE username = ?", username);
        if (!existing.isEmpty()) {
            return ApiResponse.error(409, "用户名已存在");
        }

        var hash = sha256(password);
        var userRow = jdbc.queryForMap(
            "INSERT INTO users(name, status) VALUES (?, 'active') RETURNING id",
            displayName
        );
        var userId = String.valueOf(userRow.get("id"));

        jdbc.update(
            "INSERT INTO app_user(id, username, password_hash, display_name, role) VALUES (?, ?, ?, ?, ?)",
            userId, username, hash, displayName, "user"
        );

        var result = new LinkedHashMap<String, Object>();
        result.put("userId", userId);
        result.put("username", username);
        result.put("displayName", displayName);
        result.put("role", "user");

        return ApiResponse.ok(result);
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> currentUser(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ApiResponse.error(401, "未认证");
        }
        var token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            return ApiResponse.error(401, "Token无效或已过期");
        }
        var claims = jwtUtil.parseToken(token);
        return ApiResponse.ok(claims);
    }

    private static String sha256(String input) {
        try {
            var md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private String resolveBusinessUserId(String username, String appUserId) {
        var direct = jdbc.queryForList("SELECT id FROM users WHERE id = ?", appUserId);
        if (!direct.isEmpty()) {
            return appUserId;
        }
        var fallback = switch (username) {
            case "admin" -> "user-001";
            case "zhangsan" -> "user-002";
            case "lisi" -> "user-003";
            default -> appUserId;
        };
        var mapped = jdbc.queryForList("SELECT id FROM users WHERE id = ?", fallback);
        return mapped.isEmpty() ? appUserId : fallback;
    }
}
