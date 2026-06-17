package com.example.campus.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatConversationResponse {
    private UserResponse peer;
    private String targetType;
    private Long targetId;
    private String targetTitle;
    private String lastContent;
    private LocalDateTime lastTime;
    private long unreadCount;
}
