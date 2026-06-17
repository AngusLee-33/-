package com.example.campus.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PartTimeRequest {
    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "描述不能为空")
    private String description;

    @NotNull(message = "最低薪资不能为空")
    private BigDecimal salaryMin;

    @NotNull(message = "最高薪资不能为空")
    private BigDecimal salaryMax;

    private Integer recruitCount;

    @NotBlank(message = "工作时间不能为空")
    private String workTime;

    private String requirements;

    private String location;

    private List<String> images;
}
