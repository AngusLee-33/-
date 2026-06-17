package com.example.campus.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CarpoolApplyResponse {
    private Long applyId;
    private Long carpoolId;
    private String departure;
    private String destination;
    private UserResponse applicant;
    private String status;
    private LocalDateTime createTime;
}
