package com.example.campus.service;

import com.example.campus.dto.response.NotificationResponse;
import com.example.campus.entity.Notification;
import com.example.campus.entity.User;
import com.example.campus.exception.BusinessException;
import com.example.campus.exception.ResourceNotFoundException;
import com.example.campus.repository.NotificationRepository;
import com.example.campus.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void create(Long userId, String title, String content, String type, String targetType, Long targetId) {
        if (userId == null) {
            return;
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return;
        }

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(trimToLength(title, 100));
        notification.setContent(trimToLength(content, 500));
        notification.setType(type == null || type.isBlank() ? "system" : trimToLength(type, 30));
        notification.setTargetType(targetType == null || targetType.isBlank() ? null : trimToLength(targetType, 30));
        notification.setTargetId(targetId);
        notificationRepository.save(notification);
    }

    public List<NotificationResponse> getMyNotifications(Long userId) {
        return notificationRepository.findByUser_UserIdOrderByCreateTimeDesc(userId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public long countUnread(Long userId) {
        return notificationRepository.countByUser_UserIdAndReadFlagFalse(userId);
    }

    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("通知不存在"));
        if (!notification.getUser().getUserId().equals(userId)) {
            throw new BusinessException("无权操作此通知");
        }
        notification.setReadFlag(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> notifications = notificationRepository.findByUser_UserIdOrderByCreateTimeDesc(userId);
        notifications.forEach(notification -> notification.setReadFlag(true));
        notificationRepository.saveAll(notifications);
    }

    private NotificationResponse convertToResponse(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setNotificationId(notification.getNotificationId());
        response.setTitle(notification.getTitle());
        response.setContent(notification.getContent());
        response.setType(notification.getType());
        response.setTargetType(notification.getTargetType());
        response.setTargetId(notification.getTargetId());
        response.setReadFlag(notification.getReadFlag());
        response.setCreateTime(notification.getCreateTime());
        return response;
    }

    private String trimToLength(String value, int maxLength) {
        String text = value == null || value.isBlank() ? "系统通知" : value.trim();
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }
}
