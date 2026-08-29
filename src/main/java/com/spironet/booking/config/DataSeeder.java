package com.spironet.booking.config;

import com.spironet.booking.entity.Resource;
import com.spironet.booking.entity.ResourceType;
import com.spironet.booking.entity.Role;
import com.spironet.booking.entity.User;
import com.spironet.booking.repository.ResourceRepository;
import com.spironet.booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Seeds baseline ADMIN/USER accounts and sample resources on startup so the API
 * is immediately usable without a manual bootstrap step. Idempotent - safe to
 * run against a database that already has data.
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        seedUsers();
        seedResources();
    }

    private void seedUsers() {
        createUserIfMissing("admin", "admin@bookingsystem.com", "Admin@123", Role.ADMIN);
        createUserIfMissing("user1", "user1@bookingsystem.com", "User@123", Role.USER);
        createUserIfMissing("user2", "user2@bookingsystem.com", "User@123", Role.USER);
    }

    private void createUserIfMissing(String username, String email, String rawPassword, Role role) {
        if (userRepository.existsByUsername(username)) {
            return;
        }
        userRepository.save(User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .role(role)
                .enabled(true)
                .build());
    }

    private void seedResources() {
        if (resourceRepository.count() > 0) {
            return;
        }
        resourceRepository.save(Resource.builder()
                .name("Conference Room A")
                .type(ResourceType.ROOM)
                .description("Large conference room with projector and whiteboard")
                .location("Building 1, Floor 2")
                .capacity(10)
                .pricePerHour(new BigDecimal("25.00"))
                .active(true)
                .build());

        resourceRepository.save(Resource.builder()
                .name("Company Sedan")
                .type(ResourceType.VEHICLE)
                .description("4-seat sedan for local business travel")
                .location("Parking Lot B")
                .capacity(4)
                .pricePerHour(new BigDecimal("15.00"))
                .active(true)
                .build());

        resourceRepository.save(Resource.builder()
                .name("Projector Kit")
                .type(ResourceType.EQUIPMENT)
                .description("Portable projector with HDMI and screen")
                .location("Storage Room 3")
                .pricePerHour(new BigDecimal("5.00"))
                .active(true)
                .build());
    }
}
