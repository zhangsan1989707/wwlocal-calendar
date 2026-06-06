package com.wwlocal.calendar;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CalendarApiIntegrationTest {
  static final CliPostgres postgres = CliPostgres.start();

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::jdbcUrl);
    registry.add("spring.datasource.username", () -> "wwlocal_calendar");
    registry.add("spring.datasource.password", () -> "wwlocal_calendar_password");
    registry.add("app.upload-dir", () -> "target/test-uploads");
    registry.add("app.export-dir", () -> "target/test-exports");
    registry.add("app.backup-dir", () -> "target/test-backups");
    registry.add("app.allowed-origins", () -> "http://localhost:3007");
  }

  @Autowired
  JdbcTemplate jdbc;

  @Autowired
  TestRestTemplate rest;

  @LocalServerPort
  int port;

  @BeforeEach
  void resetDatabase() throws Exception {
    var sql = Files.readString(Path.of("../database/init.sql"));
    jdbc.execute(sql);
  }

  @Test
  void protectedApisRequireAuthentication() {
    var response = rest.getForEntity(url("/api/events"), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).contains("未认证");
  }

  @Test
  void loginReturnsBusinessUserId() {
    var login = post("/api/auth/login", Map.of("username", "admin", "password", "admin123"));

    assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(login.getBody()).isNotNull();
    assertThat(login.getBody().get("success")).isEqualTo(true);

    var data = bodyData(login);
    assertThat(data.get("userId")).isEqualTo("user-001");
    assertThat(data.get("role")).isEqualTo("admin");
    assertThat(String.valueOf(data.get("token"))).isNotBlank();
  }

  @Test
  void eventCrudPersistsAndUsesCurrentSchema() {
    var token = loginToken();
    var headers = authHeaders(token);
    var createPayload = new LinkedHashMap<String, Object>();
    createPayload.put("title", "集成测试日程");
    createPayload.put("calendar_id", "cal-001");
    createPayload.put("organizer_user_id", "user-001");
    createPayload.put("start_at", "2026-06-20T09:00:00+08:00");
    createPayload.put("end_at", "2026-06-20T10:00:00+08:00");
    createPayload.put("location", "测试会议室");
    createPayload.put("description", "验证 schema 与 CRUD 契约");
    createPayload.put("visibility", "DEFAULT");
    createPayload.put("timezone", "Asia/Shanghai");
    createPayload.put("participantIds", List.of("user-002"));
    createPayload.put("editorUserIds", List.of("user-002"));
    createPayload.put("reminders", List.of(Map.of("minutes_before", 15, "method", "SYSTEM")));
    createPayload.put("todos", List.of(Map.of("title", "确认测试通过", "priority", "HIGH")));
    createPayload.put("rrule", "FREQ=WEEKLY;BYDAY=MO");
    createPayload.put("occurrence_count", 2);
    createPayload.put("operatorUserId", "user-001");

    var created = exchange("/api/events", HttpMethod.POST, createPayload, headers);

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
    var eventId = ((Number) bodyData(created).get("id")).longValue();

    var participantRole = jdbc.queryForObject(
        "SELECT role FROM event_participant WHERE event_id = ? AND user_id = 'user-002'",
        String.class, eventId);
    assertThat(participantRole).isEqualTo("EDITOR");

    var listed = exchangeList("/api/events?keyword=集成测试日程&userId=user-001", headers);
    assertThat(listed.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(listed.getBody()).isNotNull();
    var events = bodyDataList(listed);
    assertThat(events).extracting(row -> row.get("title")).contains("集成测试日程");

    var updatePayload = Map.of(
        "title", "集成测试日程已更新",
        "calendar_id", "cal-001",
        "organizer_user_id", "user-001",
        "start_at", "2026-06-20T11:00:00+08:00",
        "end_at", "2026-06-20T12:00:00+08:00",
        "timezone", "Asia/Shanghai",
        "operatorUserId", "user-001");
    var updated = exchange("/api/events/" + eventId, HttpMethod.PUT, updatePayload, headers);

    assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(bodyData(updated).get("title")).isEqualTo("集成测试日程已更新");

    var deleted = exchange("/api/events/" + eventId + "?operatorUserId=user-001&scope=series",
        HttpMethod.DELETE, null, headers);

    assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.OK);
    var status = jdbc.queryForObject("SELECT status FROM event WHERE id = ?", String.class, eventId);
    assertThat(status).isEqualTo("CANCELLED");
  }

  private String loginToken() {
    return String.valueOf(bodyData(post("/api/auth/login",
        Map.of("username", "admin", "password", "admin123"))).get("token"));
  }

  private HttpHeaders authHeaders(String token) {
    var headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return headers;
  }

  private ResponseEntity<Map<String, Object>> post(String path, Object payload) {
    return rest.exchange(url(path), HttpMethod.POST, new HttpEntity<>(payload),
        new ParameterizedTypeReference<>() {});
  }

  private ResponseEntity<Map<String, Object>> exchange(
      String path, HttpMethod method, Object payload, HttpHeaders headers) {
    return rest.exchange(url(path), method, new HttpEntity<>(payload, headers),
        new ParameterizedTypeReference<>() {});
  }

  private ResponseEntity<Map<String, Object>> exchangeList(String path, HttpHeaders headers) {
    return rest.exchange(url(path), HttpMethod.GET, new HttpEntity<>(headers),
        new ParameterizedTypeReference<>() {});
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> bodyData(ResponseEntity<Map<String, Object>> response) {
    return (Map<String, Object>) response.getBody().get("data");
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> bodyDataList(ResponseEntity<Map<String, Object>> response) {
    return (List<Map<String, Object>>) response.getBody().get("data");
  }

  private String url(String path) {
    return "http://localhost:" + port + path;
  }

  private record CliPostgres(String containerId, int port) {
    static CliPostgres start() {
      var port = freePort();
      var containerId = run(
          "docker", "run", "--rm", "-d",
          "-e", "POSTGRES_DB=wwlocal_calendar",
          "-e", "POSTGRES_USER=wwlocal_calendar",
          "-e", "POSTGRES_PASSWORD=wwlocal_calendar_password",
          "-p", "127.0.0.1:" + port + ":5432",
          "postgres:16-alpine").trim();
      if (containerId.isBlank()) {
        throw new IllegalStateException("无法启动 PostgreSQL 测试容器");
      }
      Runtime.getRuntime().addShutdownHook(new Thread(() -> runQuietly("docker", "stop", containerId)));
      waitUntilReady(containerId);
      return new CliPostgres(containerId, port);
    }

    String jdbcUrl() {
      return "jdbc:postgresql://127.0.0.1:" + port + "/wwlocal_calendar";
    }

    private static int freePort() {
      try (var socket = new ServerSocket(0)) {
        return socket.getLocalPort();
      } catch (IOException ex) {
        throw new IllegalStateException("无法分配 PostgreSQL 测试端口", ex);
      }
    }

    private static void waitUntilReady(String containerId) {
      var deadline = System.nanoTime() + Duration.ofSeconds(60).toNanos();
      while (System.nanoTime() < deadline) {
        var result = runQuietly(
            "docker", "exec", containerId,
            "pg_isready", "-U", "wwlocal_calendar", "-d", "wwlocal_calendar");
        if (result.contains("accepting connections")) {
          return;
        }
        sleep();
      }
      var logs = runQuietly("docker", "logs", containerId);
      runQuietly("docker", "stop", containerId);
      throw new IllegalStateException("PostgreSQL 测试容器未就绪:\n" + logs);
    }

    private static String run(String... command) {
      try {
        var process = new ProcessBuilder(command).redirectErrorStream(true).start();
        var output = new String(process.getInputStream().readAllBytes());
        var exit = process.waitFor();
        if (exit != 0) {
          throw new IllegalStateException(String.join(" ", command) + " failed:\n" + output);
        }
        return output;
      } catch (IOException ex) {
        throw new IllegalStateException("无法执行命令: " + String.join(" ", command), ex);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("命令被中断: " + String.join(" ", command), ex);
      }
    }

    private static String runQuietly(String... command) {
      try {
        return run(command);
      } catch (RuntimeException ex) {
        return ex.getMessage();
      }
    }

    private static void sleep() {
      try {
        Thread.sleep(500);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("等待 PostgreSQL 测试容器时被中断", ex);
      }
    }
  }
}
