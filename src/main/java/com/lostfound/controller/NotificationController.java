
package com.lostfound.controller;

import com.lostfound.dto.notification.NotificationResponse;
import com.lostfound.service.NotificationService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(
            NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(
            Authentication authentication) {

        return ResponseEntity.ok(
                notificationService.getMyNotifications(
                        authentication.getName()));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            Authentication authentication) {

        return ResponseEntity.ok(
                Map.of("unreadCount",
                        notificationService.getUnreadCount(
                                authentication.getName())));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            Authentication authentication,
            @PathVariable Long notificationId) {

        return ResponseEntity.ok(
                notificationService.markAsRead(
                        authentication.getName(), notificationId));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, String>> markAllAsRead(
            Authentication authentication) {

        notificationService.markAllAsRead(authentication.getName());

        return ResponseEntity.ok(
                Map.of("message", "All notifications marked as read"));
    }
}
