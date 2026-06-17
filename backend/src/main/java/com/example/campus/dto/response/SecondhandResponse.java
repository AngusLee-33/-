package com.example.campus.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SecondhandResponse {
    private Long secondhandId;
    private UserResponse user;
    private String title;
    private String description;
    private BigDecimal price;
    private String category;
    private List<String> images;
    private String status;
    private LocalDateTime createTime;
}
