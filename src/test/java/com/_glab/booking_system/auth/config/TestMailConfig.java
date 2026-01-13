package com._glab.booking_system.auth.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Test configuration that provides a mock JavaMailSender.
 * Prevents actual emails from being sent during tests.
 */
@TestConfiguration
public class TestMailConfig {

    @Bean
    @Primary
    public JavaMailSender javaMailSender() {
        // Return a mock that does nothing when send() is called
        return Mockito.mock(JavaMailSender.class);
    }
}

