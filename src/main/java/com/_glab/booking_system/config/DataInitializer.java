package com._glab.booking_system.config;

import com._glab.booking_system.user.model.Role;
import com._glab.booking_system.user.model.RoleName;
import com._glab.booking_system.user.model.User;
import com._glab.booking_system.user.repository.RoleRepository;
import com._glab.booking_system.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Initializes the database with seed data for development/testing.
 * Only runs when "dev" profile is active.
 * <p>
 * TODO (PRODUCTION): DELETE THIS ENTIRE FILE BEFORE DEPLOYMENT!
 * This file creates test users with known passwords which is a security risk.
 * Also delete any test users created in the database.
 */
@Configuration
@Slf4j
public class DataInitializer {

    @Bean
    @Profile("dev")
    CommandLineRunner initData(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {
            // Create roles if they don't exist
            for (RoleName roleName : RoleName.values()) {
                if (roleRepository.findByName(roleName).isEmpty()) {
                    Role role = new Role();
                    role.setName(roleName);
                    role.setDescription(roleName.name() + " role");
                    roleRepository.save(role);
                    DataInitializer.log.info("Created role: {}", roleName);
                }
            }

            // Create admin user if doesn't exist
            String adminEmail = "admin@5glab.com";
            if (userRepository.findByEmail(adminEmail).isEmpty()) {
                Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                        .orElseThrow(() -> new RuntimeException("Admin role not found"));

                User admin = new User();
                admin.setEmail(adminEmail);
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setFirstName("Admin");
                admin.setLastName("User");
                admin.setRole(adminRole);
                admin.setEnabled(true);
                admin.setMfaEnabled(false); // Disable MFA for easy testing
                userRepository.save(admin);

                DataInitializer.log.info("===========================================");
                DataInitializer.log.info("Created test admin user:");
                DataInitializer.log.info("  Email: {}", adminEmail);
                DataInitializer.log.info("  Password: admin123");
                DataInitializer.log.info("===========================================");
            }

            // Create a professor user for testing
            String professorEmail = "professor@5glab.com";
            if (userRepository.findByEmail(professorEmail).isEmpty()) {
                Role professorRole = roleRepository.findByName(RoleName.PROFESSOR)
                        .orElseThrow(() -> new RuntimeException("Professor role not found"));

                User professor = new User();
                professor.setEmail(professorEmail);
                professor.setUsername("professor");
                professor.setPassword(passwordEncoder.encode("prof123"));
                professor.setFirstName("John");
                professor.setLastName("Doe");
                professor.setRole(professorRole);
                professor.setEnabled(true);
                professor.setMfaEnabled(false);
                userRepository.save(professor);

                DataInitializer.log.info("Created test professor user:");
                DataInitializer.log.info("  Email: {}", professorEmail);
                DataInitializer.log.info("  Password: prof123");
                DataInitializer.log.info("===========================================");
            }
        };
    }

    /**
     * Ensures the anonymous system user exists (for placeholder when users are hard-deleted).
     * Runs on all profiles.
     */
    @Bean
    CommandLineRunner initAnonymousUser(UserRepository userRepository, RoleRepository roleRepository) {
        return args -> {
            if (userRepository.findByIsAnonymousTrue().isPresent()) {
                return;
            }
            Role professorRole = roleRepository.findByName(RoleName.PROFESSOR)
                    .orElseGet(() -> {
                        Role r = new Role();
                        r.setName(RoleName.PROFESSOR);
                        r.setDescription("PROFESSOR role");
                        return roleRepository.save(r);
                    });
            User anonymous = new User();
            anonymous.setEmail("anonymous@system.local");
            anonymous.setUsername("__anonymous__");
            anonymous.setFirstName("Deleted");
            anonymous.setLastName("User");
            anonymous.setRole(professorRole);
            anonymous.setEnabled(false);
            anonymous.setIsAnonymous(true);
            userRepository.save(anonymous);
            DataInitializer.log.info("Created anonymous system user (placeholder for deleted accounts)");
        };
    }
}






