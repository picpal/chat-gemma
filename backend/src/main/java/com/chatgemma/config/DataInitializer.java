package com.chatgemma.config;

import com.chatgemma.entity.User;
import com.chatgemma.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Check if admin user already exists
        if (!userRepository.existsByEmail("admin@chatgemma.com")) {
            String encodedPassword = passwordEncoder.encode("admin123");
            User admin = User.createAdmin("admin", encodedPassword, "admin@chatgemma.com");

            userRepository.save(admin);
            System.out.println("=== Initial Admin User Created ===");
            System.out.println("Email: admin@chatgemma.com");
            System.out.println("Password: admin123");
            System.out.println("================================");
        }
    }
}