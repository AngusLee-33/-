package com.example.campus.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CarpoolRequest {
    @NotBlank(message = "出发地不能为空")
    private String departure;

    @NotBlank(message = "目的地不能为空")
    private String destination;

    @NotNull(message = "出发时间不能为空")
    private LocalDateTime departureTime;

    @NotNull(message = "座位数不能为空")
    private Integer seats;

    @NotNull(message = "价格不能为空")
    private BigDecimal price;

    private String description;

    private List<String> images;
}
