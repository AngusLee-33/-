package com.example.campus.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderResponse {
    private Long orderId;
    private String type;
    private Long targetId;
    private BigDecimal amount;
    private String status;
    private UserResponse user;
    private UserResponse seller;
    private String targetTitle;
    private String targetImage;
    private LocalDateTime payTime;
    private LocalDateTime completeTime;
    private LocalDateTime createTime;
}
