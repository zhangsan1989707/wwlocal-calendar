package com.wwlocal.calendar.controller;

import com.wwlocal.calendar.api.ApiResponse;
import com.wwlocal.calendar.service.EventService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class EventController {
    private final EventService events;

    public EventController(EventService events) {
        this.events = events;
    }

    @GetMapping("/events")
    public ApiResponse<List<Map<String, Object>>> list(@RequestParam Map<String, String> params) {
        return ApiResponse.ok(events.search(params));
    }

    @PostMapping("/events")
    public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> payload) {
        return ApiResponse.ok(events.save(payload));
    }

    @PutMapping("/events/{id}")
    public ApiResponse<Map<String, Object>> update(@PathVariable long id, @RequestBody Map<String, Object> payload) {
        payload.put("id", id);
        return ApiResponse.ok(events.save(payload));
    }

    @DeleteMapping("/events/{id}")
    public ApiResponse<Void> delete(@PathVariable long id,
                                    @RequestParam(required = false) Long operatorUserId,
                                    @RequestParam(required = false, defaultValue = "single") String scope) {
        events.remove(id, operatorUserId, scope);
        return ApiResponse.ok();
    }

    @PostMapping("/events/{id}/respond")
    public ApiResponse<Void> respond(@PathVariable long id, @RequestBody Map<String, Object> payload) {
        events.respond(id, ((Number) payload.get("userId")).longValue(), String.valueOf(payload.get("status")));
        return ApiResponse.ok();
    }

    @PostMapping("/freebusy/query")
    public ApiResponse<List<Map<String, Object>>> freebusy(@RequestBody Map<String, Object> payload) {
        return ApiResponse.ok(events.freebusy(payload));
    }

    @GetMapping("/events/{id}/attachments")
    public ApiResponse<List<Map<String, Object>>> getEventAttachments(@PathVariable long id) {
        return ApiResponse.ok(events.getEventAttachments(id));
    }

    @GetMapping("/events/{id}/participants")
    public ApiResponse<List<Map<String, Object>>> getEventParticipants(@PathVariable long id) {
        return ApiResponse.ok(events.getEventParticipants(id));
    }

    @GetMapping("/events/{id}/reminders")
    public ApiResponse<List<Map<String, Object>>> getEventReminders(@PathVariable long id) {
        return ApiResponse.ok(events.getEventReminders(id));
    }

    @GetMapping("/events/{id}/todos")
    public ApiResponse<List<Map<String, Object>>> getEventTodos(@PathVariable long id) {
        return ApiResponse.ok(events.getEventTodos(id));
    }

    @PutMapping("/todos/{todoId}")
    public ApiResponse<Map<String, Object>> toggleTodo(@PathVariable long todoId, @RequestBody Map<String, Object> payload) {
        boolean completed = Boolean.TRUE.equals(payload.get("completed"));
        Long operatorUserId = payload.get("operatorUserId") != null ? ((Number) payload.get("operatorUserId")).longValue() : null;
        return ApiResponse.ok(events.toggleTodo(todoId, completed, operatorUserId));
    }

    @PostMapping("/export/events")
    public void exportEvents(@RequestBody Map<String, Object> payload, HttpServletResponse response) throws IOException {
        Long operatorUserId = payload.get("operatorUserId") != null 
            ? ((Number) payload.get("operatorUserId")).longValue() 
            : null;
        
        try {
            Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
            Path filePath = events.exportEvents(tempDir, operatorUserId);
            
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, 
                "attachment; filename=\"" + filePath.getFileName().toString() + "\"");
            
            try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
                FileCopyUtils.copy(fis, response.getOutputStream());
            }
        } catch (Exception e) {
            throw new IOException("导出日程失败: " + e.getMessage(), e);
        }
    }
}
