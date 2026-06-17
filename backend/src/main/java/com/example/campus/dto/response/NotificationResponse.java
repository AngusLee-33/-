package com.example.campus.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationResponse {
    private Long notificationId;
    private String title;
    private String content;
    private String type;
    private String targetType;
    private Long targetId;
    private Boolean readFlag;
    private LocalDateTime createTime;
}
