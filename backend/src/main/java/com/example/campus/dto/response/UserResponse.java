package com.example.campus.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserResponse {
    private Long userId;
    private String username;
    private String realName;
    private String phone;
    private String email;
    private String idCard;
    private String role;
    private String status;
    private String avatar;
    private LocalDateTime createTime;
}
