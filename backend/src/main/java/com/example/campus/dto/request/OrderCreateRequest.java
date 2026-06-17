package com.example.campus.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderCreateRequest {
    @NotBlank(message = "订单类型不能为空")
    private String type;

    @NotNull(message = "关联ID不能为空")
    private Long targetId;

    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.00", message = "金额不能小于 0")
    private BigDecimal amount;
}
