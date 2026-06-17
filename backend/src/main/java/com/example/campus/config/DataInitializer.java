package com.example.campus.config;

import com.example.campus.entity.User;
import com.example.campus.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;

    public DataInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.username:admin}") String adminUsername,
            @Value("${app.admin.password:123456}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        userRepository.findByUsername(adminUsername).ifPresentOrElse(
                user -> {
                    if (!"admin".equals(user.getRole())) {
                        user.setRole("admin");
                        user.setStatus("active");
                        userRepository.save(user);
                    }
                },
                () -> {
                    User admin = new User();
                    admin.setUsername(adminUsername);
                    admin.setPassword(passwordEncoder.encode(adminPassword));
                    admin.setRealName("系统管理员");
                    admin.setRole("admin");
                    admin.setStatus("active");
                    userRepository.save(admin);
                }
        );
    }
}
