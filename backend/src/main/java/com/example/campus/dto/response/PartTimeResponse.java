package com.example.campus.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PartTimeResponse {
    private Long partTimeId;
    private UserResponse merchant;
    private String title;
    private String description;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private Integer recruitCount;
    private Long acceptedCount;
    private String workTime;
    private String requirements;
    private String location;
    private List<String> images;
    private String status;
    private LocalDateTime createTime;
}
