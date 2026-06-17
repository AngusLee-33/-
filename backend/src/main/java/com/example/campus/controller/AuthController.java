package com.example.campus.controller;

import com.example.campus.dto.request.ForgotPasswordCodeRequest;
import com.example.campus.dto.request.LoginRequest;
import com.example.campus.dto.request.RegisterRequest;
import com.example.campus.dto.request.ResetPasswordRequest;
import com.example.campus.dto.response.ApiResponse;
import com.example.campus.dto.response.LoginResponse;
import com.example.campus.dto.response.UserResponse;
import com.example.campus.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("注册成功", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return ResponseEntity.ok(ApiResponse.success("登录成功", response));
    }

    @PostMapping("/password/code")
    public ResponseEntity<ApiResponse<String>> sendResetPasswordCode(@Valid @RequestBody ForgotPasswordCodeRequest request) {
        String code = userService.sendResetPasswordCode(request.getPhone());
        return ResponseEntity.ok(ApiResponse.success("验证码已发送", code));
    }

    @PostMapping("/password/reset")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("密码重置成功", null));
    }
}
