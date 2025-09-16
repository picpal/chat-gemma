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
        System.out.println("🚀 [DataInitializer] Starting data initialization...");

        try {
            // Check if admin user already exists
            boolean adminExists = userRepository.existsByEmail("admin@chatgemma.com");
            System.out.println("🔍 [DataInitializer] Admin user exists: " + adminExists);

            if (!adminExists) {
                System.out.println("🛠️ [DataInitializer] Creating admin user...");
                String encodedPassword = passwordEncoder.encode("admin123");
                User admin = User.createAdmin("admin", encodedPassword, "admin@chatgemma.com");

                User savedAdmin = userRepository.save(admin);
                System.out.println("=== Initial Admin User Created ===");
                System.out.println("ID: " + savedAdmin.getId());
                System.out.println("Email: admin@chatgemma.com");
                System.out.println("Password: admin123");
                System.out.println("Role: " + savedAdmin.getRole());
                System.out.println("Status: " + savedAdmin.getStatus());
                System.out.println("================================");
            } else {
                System.out.println("✅ [DataInitializer] Admin user already exists, skipping creation");
            }
        } catch (Exception e) {
            System.err.println("❌ [DataInitializer] Error during initialization: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}