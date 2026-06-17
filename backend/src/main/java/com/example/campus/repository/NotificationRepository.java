package com.example.campus.repository;

import com.example.campus.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUser_UserIdOrderByCreateTimeDesc(Long userId);

    long countByUser_UserIdAndReadFlagFalse(Long userId);

    void deleteByUser_UserId(Long userId);
}
