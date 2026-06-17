package com.example.campus.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CarpoolResponse {
    private Long carpoolId;
    private UserResponse user;
    private String departure;
    private String destination;
    private LocalDateTime departureTime;
    private Integer seats;
    private Long acceptedCount;
    private BigDecimal price;
    private String description;
    private List<String> images;
    private String status;
    private LocalDateTime createTime;
}
