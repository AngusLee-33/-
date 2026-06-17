package com.example.campus.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PartTimeApplyResponse {
    private Long applyId;
    private Long partTimeId;
    private String title;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private Integer recruitCount;
    private Long acceptedCount;
    private String workTime;
    private String location;
    private String resume;
    private UserResponse applicant;
    private String status;
    private LocalDateTime createTime;
}
