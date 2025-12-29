package com._glab.booking_system.auth.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * Test configuration that provides a mock JavaMailSender.
 * Prevents actual emails from being sent during tests.
 */
@TestConfiguration
public class TestMailConfig {

    @Bean
    @Primary
    public JavaMailSender javaMailSender() {
        // Return a simple implementation that won't actually send emails
        // In tests, the @Async email sending will just complete without sending
        return new JavaMailSenderImpl();
    }
}

