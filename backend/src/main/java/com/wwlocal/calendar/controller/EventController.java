package com.wwlocal.calendar.controller;

import com.wwlocal.calendar.api.ApiResponse;
import com.wwlocal.calendar.service.EventService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
  public ApiResponse<Void> delete(@PathVariable long id, @RequestParam(required = false) Long operatorUserId) {
    events.remove(id, operatorUserId);
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
}
