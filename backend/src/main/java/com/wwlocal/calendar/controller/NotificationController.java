package com.wwlocal.calendar.controller;

import com.wwlocal.calendar.api.ApiResponse;
import com.wwlocal.calendar.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/notifications")
    public ApiResponse<Map<String, Object>> list(HttpServletRequest request,
                                                  @RequestParam(defaultValue = "50") int limit) {
        var userId = (String) request.getAttribute("currentUserId");
        var notifications = notificationService.getNotifications(userId, limit);
        var unreadCount = notificationService.getUnreadCount(userId);
        return ApiResponse.ok(Map.of("notifications", notifications, "unreadCount", unreadCount));
    }

    @PutMapping("/notifications/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable long id) {
        notificationService.markAsRead(id);
        return ApiResponse.ok(null);
    }

    @PutMapping("/notifications/read-all")
    public ApiResponse<Void> markAllRead(HttpServletRequest request) {
        var userId = (String) request.getAttribute("currentUserId");
        notificationService.markAllAsRead(userId);
        return ApiResponse.ok(null);
    }
}