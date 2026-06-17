package com.example.campus.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReviewResponse {
    private Long reviewId;
    private String type;
    private Long orderId;
    private UserResponse reviewer;
    private UserResponse reviewee;
    private Integer score;
    private String content;
    private LocalDateTime createTime;
}
