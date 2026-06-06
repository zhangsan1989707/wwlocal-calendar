package com.wwlocal.calendar.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    private final JdbcTemplate jdbc;

    public NotificationService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void createNotification(String userId, Long eventId, String title, String message, String type) {
        jdbc.update(
            "INSERT INTO notification(user_id, event_id, title, message, notification_type) VALUES (?, ?, ?, ?, ?)",
            userId, eventId, title, message, type
        );
    }

    public void notifyEventUpdate(Long eventId, String organizerUserId, String eventTitle) {
        // 通知所有参与人（除了发起人自己）
        var participants = jdbc.queryForList(
            "SELECT user_id FROM event_participant WHERE event_id = ? AND user_id IS NOT NULL AND user_id != ?",
            eventId, organizerUserId
        );
        for (var p : participants) {
            var userId = String.valueOf(p.get("user_id"));
            createNotification(userId, eventId, "日程已更新",
                "日程「" + eventTitle + "」已被发起人更新", "event_update");
        }
    }

    public void markAsRead(long notificationId) {
        jdbc.update("UPDATE notification SET is_read = true WHERE id = ?", notificationId);
    }

    public void markAllAsRead(String userId) {
        jdbc.update("UPDATE notification SET is_read = true WHERE user_id = ? AND is_read = false", userId);
    }

    public List<Map<String, Object>> getNotifications(String userId, int limit) {
        return jdbc.queryForList(
            "SELECT * FROM notification WHERE user_id = ? ORDER BY created_at DESC LIMIT ?",
            userId, limit
        );
    }

    public int getUnreadCount(String userId) {
        var count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM notification WHERE user_id = ? AND is_read = false",
            Integer.class, userId
        );
        return count != null ? count : 0;
    }
}