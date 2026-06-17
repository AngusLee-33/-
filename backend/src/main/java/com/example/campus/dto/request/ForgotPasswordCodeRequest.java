package com.example.campus.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordCodeRequest {
    @NotBlank(message = "手机号不能为空")
    private String phone;
}
