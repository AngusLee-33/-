package com.example.campus.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessageResponse {
    private Long messageId;
    private UserResponse sender;
    private UserResponse receiver;
    private String targetType;
    private Long targetId;
    private String content;
    private Boolean readFlag;
    private Boolean mine;
    private LocalDateTime createTime;
}
