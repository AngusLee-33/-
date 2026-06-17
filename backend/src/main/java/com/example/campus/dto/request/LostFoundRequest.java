package com.example.campus.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class LostFoundRequest {

    @NotBlank(message = "类型不能为空")
    private String type;

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "描述不能为空")
    private String description;

    @NotBlank(message = "地点不能为空")
    private String location;

    private LocalDateTime eventTime;

    @NotBlank(message = "联系方式不能为空")
    private String contact;

    private List<String> images;
}
