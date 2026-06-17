package com.example.campus.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class LostFoundResponse {
    private Long lostFoundId;
    private UserResponse user;
    private String type;
    private String title;
    private String description;
    private String location;
    private LocalDateTime eventTime;
    private String contact;
    private List<String> images;
    private String status;
    private LocalDateTime createTime;
}
