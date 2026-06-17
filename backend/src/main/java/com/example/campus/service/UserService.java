package com.example.campus.service;

import com.example.campus.dto.request.LoginRequest;
import com.example.campus.dto.request.RegisterRequest;
import com.example.campus.dto.request.ResetPasswordRequest;
import com.example.campus.dto.request.UpdateProfileRequest;
import com.example.campus.dto.response.LoginResponse;
import com.example.campus.dto.response.UserResponse;
import com.example.campus.entity.User;
import com.example.campus.exception.BusinessException;
import com.example.campus.exception.ResourceNotFoundException;
import com.example.campus.repository.UserRepository;
import com.example.campus.config.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserService {

    private static final int RESET_CODE_EXPIRE_MINUTES = 10;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final SmsService smsService;
    private final Map<String, ResetCode> resetCodes = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            SmsService smsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.smsService = smsService;
    }

    public UserResponse register(RegisterRequest request) {
        String phone = normalizeBlank(request.getPhone());
        String email = normalizeBlank(request.getEmail());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("用户名已存在");
        }
        if (phone != null && userRepository.existsByPhone(phone)) {
            throw new BusinessException("手机号已被注册");
        }
        if (email != null && userRepository.existsByEmail(email)) {
            throw new BusinessException("邮箱已被注册");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setPhone(phone);
        user.setEmail(email);
        user.setRole(request.getRole());

        User savedUser = userRepository.save(user);
        return convertToResponse(savedUser);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException("用户名或密码错误"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        if (!"active".equals(user.getStatus())) {
            throw new BusinessException("账号已被禁用");
        }

        String token = jwtTokenProvider.generateToken(user.getUserId(), user.getUsername(), user.getRole());
        UserResponse userResponse = convertToResponse(user);

        return new LoginResponse(token, userResponse);
    }

    public String sendResetPasswordCode(String phone) {
        String normalizedPhone = normalizeBlank(phone);
        if (normalizedPhone == null) {
            throw new BusinessException("请输入手机号");
        }

        userRepository.findByPhone(normalizedPhone)
                .orElseThrow(() -> new BusinessException("该手机号未注册"));

        String code = String.format("%06d", random.nextInt(1_000_000));
        resetCodes.put(normalizedPhone, new ResetCode(code, LocalDateTime.now().plusMinutes(RESET_CODE_EXPIRE_MINUTES)));
        if (smsService.isConfigured()) {
            try {
                smsService.sendResetPasswordCode(normalizedPhone, code);
                return null;
            } catch (Exception error) {
                resetCodes.remove(normalizedPhone);
                throw new BusinessException("短信发送失败：" + error.getMessage());
            }
        }
        return code;
    }

    public void resetPassword(ResetPasswordRequest request) {
        String phone = normalizeBlank(request.getPhone());
        String code = normalizeBlank(request.getCode());
        String newPassword = normalizeBlank(request.getNewPassword());

        if (phone == null || code == null || newPassword == null) {
            throw new BusinessException("手机号、验证码和新密码不能为空");
        }
        if (newPassword.length() < 6 || newPassword.length() > 50) {
            throw new BusinessException("密码长度必须在 6 到 50 位之间");
        }

        ResetCode resetCode = resetCodes.get(phone);
        if (resetCode == null) {
            throw new BusinessException("请先获取验证码");
        }
        if (resetCode.expireTime().isBefore(LocalDateTime.now())) {
            resetCodes.remove(phone);
            throw new BusinessException("验证码已过期，请重新获取");
        }
        if (!resetCode.code().equals(code)) {
            throw new BusinessException("验证码错误");
        }

        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new BusinessException("该手机号未注册"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        resetCodes.remove(phone);
    }

    private String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        return convertToResponse(user);
    }

    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        String phone = normalizeBlank(request.getPhone());
        String email = normalizeBlank(request.getEmail());
        String realName = request.getRealName().trim();

        if (phone != null) {
            userRepository.findByPhone(phone)
                    .filter(existing -> !existing.getUserId().equals(userId))
                    .ifPresent(existing -> {
                        throw new BusinessException("手机号已被其他用户使用");
                    });
        }
        if (email != null) {
            userRepository.findByEmail(email)
                    .filter(existing -> !existing.getUserId().equals(userId))
                    .ifPresent(existing -> {
                        throw new BusinessException("邮箱已被其他用户使用");
                    });
        }

        user.setRealName(realName);
        user.setPhone(phone);
        user.setEmail(email);

        User savedUser = userRepository.save(user);
        return convertToResponse(savedUser);
    }

    public User getCurrentUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
    }

    private UserResponse convertToResponse(User user) {
        UserResponse response = new UserResponse();
        response.setUserId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setPhone(user.getPhone());
        response.setEmail(user.getEmail());
        response.setIdCard(user.getIdCard());
        response.setRole(user.getRole());
        response.setStatus(user.getStatus());
        response.setAvatar(user.getAvatar());
        response.setCreateTime(user.getCreateTime());
        return response;
    }

    private record ResetCode(String code, LocalDateTime expireTime) {
    }
}
