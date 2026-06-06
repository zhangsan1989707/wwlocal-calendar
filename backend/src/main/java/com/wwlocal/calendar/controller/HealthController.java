package com.wwlocal.calendar.controller;

import com.wwlocal.calendar.api.ApiResponse;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {
  private final JdbcTemplate jdbc;

  public HealthController(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @GetMapping("/health")
  public ApiResponse<Map<String, Object>> health() {
    jdbc.queryForObject("SELECT 1", Integer.class);
    return ApiResponse.ok(Map.of("status", "UP", "database", "UP"));
  }
}
