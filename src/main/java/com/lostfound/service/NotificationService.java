
package com.lostfound.service;

import com.lostfound.dto.notification.NotificationResponse;
import com.lostfound.entity.Notification;
import com.lostfound.entity.User;
import com.lostfound.repository.NotificationRepository;
import com.lostfound.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    public List<NotificationResponse> getMyNotifications(String email) {
        User user = getUser(email);

        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public long getUnreadCount(String email) {
        User user = getUser(email);
        return notificationRepository.countByUserIdAndReadFalse(user.getId());
    }

    @Transactional
    public NotificationResponse markAsRead(String email, Long notificationId) {
        User user = getUser(email);

        Notification notification =
                notificationRepository.findById(notificationId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Notification not found"));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException(
                    "You cannot access another user's notification");
        }

        notification.setRead(true);
        return toResponse(notificationRepository.save(notification));
    }

    @Transactional
    public void markAllAsRead(String email) {
        User user = getUser(email);

        List<Notification> notifications = notificationRepository
                .findByUserIdAndReadFalseOrderByCreatedAtDesc(user.getId());

        notifications.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(notifications);
    }

    @Transactional
    public NotificationResponse createNotification(
            Long userId, String title, String message, String type) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setRead(false);

        return toResponse(notificationRepository.save(notification));
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));
    }

    private NotificationResponse toResponse(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        response.setTitle(notification.getTitle());
        response.setMessage(notification.getMessage());
        response.setType(notification.getType());
        response.setRead(notification.isRead());
        response.setCreatedAt(notification.getCreatedAt());
        return response;
    }
}
