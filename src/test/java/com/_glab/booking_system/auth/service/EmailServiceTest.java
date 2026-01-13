package com._glab.booking_system.auth.service;

import com._glab.booking_system.auth.config.AppProperties;
import com._glab.booking_system.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private AppProperties appProperties;
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.getMail().setFrom("test@5glab.edu.pl");
        appProperties.getFrontend().setUrl("http://localhost:3000");

        emailService = new EmailService(mailSender, appProperties);
    }

    @Nested
    @DisplayName("Send Account Setup Email Tests")
    class SendAccountSetupEmailTests {

        @Test
        @DisplayName("Should send account setup email with correct content")
        void shouldSendAccountSetupEmail() {
            // Given
            User user = new User();
            user.setEmail("newuser@example.com");
            user.setFirstName("John");
            user.setLastName("Doe");

            String token = "test-setup-token-123";

            // When
            emailService.sendAccountSetupEmail(user, token);

            // Then
            ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(messageCaptor.capture());

            SimpleMailMessage sentMessage = messageCaptor.getValue();
            assertThat(sentMessage.getFrom()).isEqualTo("test@5glab.edu.pl");
            assertThat(sentMessage.getTo()).containsExactly("newuser@example.com");
            assertThat(sentMessage.getSubject()).contains("Complete Your Account Setup");
            assertThat(sentMessage.getText()).contains("John Doe");
            assertThat(sentMessage.getText()).contains("http://localhost:3000/setup-password?token=test-setup-token-123");
            assertThat(sentMessage.getText()).contains("48 hours");
        }
    }

    @Nested
    @DisplayName("Send OTP Email Tests")
    class SendOtpEmailTests {

        @Test
        @DisplayName("Should send OTP email with correct content")
        void shouldSendOtpEmail() {
            // Given
            String email = "user@example.com";
            String otp = "123456";
            long expiryMinutes = 10;

            // When
            emailService.sendOtpEmail(email, otp, expiryMinutes);

            // Then
            ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(messageCaptor.capture());

            SimpleMailMessage sentMessage = messageCaptor.getValue();
            assertThat(sentMessage.getFrom()).isEqualTo("test@5glab.edu.pl");
            assertThat(sentMessage.getTo()).containsExactly("user@example.com");
            assertThat(sentMessage.getSubject()).contains("Verification Code");
            assertThat(sentMessage.getText()).contains("123456");
            assertThat(sentMessage.getText()).contains("10 minutes");
        }
    }

    @Nested
    @DisplayName("Send Email Tests")
    class SendEmailTests {

        @Test
        @DisplayName("Should send generic email")
        void shouldSendGenericEmail() {
            // Given
            String to = "recipient@example.com";
            String subject = "Test Subject";
            String body = "Test body content";

            // When
            emailService.sendEmail(to, subject, body);

            // Then
            ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(messageCaptor.capture());

            SimpleMailMessage sentMessage = messageCaptor.getValue();
            assertThat(sentMessage.getFrom()).isEqualTo("test@5glab.edu.pl");
            assertThat(sentMessage.getTo()).containsExactly("recipient@example.com");
            assertThat(sentMessage.getSubject()).isEqualTo("Test Subject");
            assertThat(sentMessage.getText()).isEqualTo("Test body content");
        }

        @Test
        @DisplayName("Should throw exception when mail sending fails")
        void shouldThrowExceptionWhenMailFails() {
            // Given
            doThrow(new MailSendException("SMTP connection failed"))
                    .when(mailSender).send(any(SimpleMailMessage.class));

            // When/Then
            assertThatThrownBy(() -> emailService.sendEmail("to@example.com", "Subject", "Body"))
                    .isInstanceOf(MailSendException.class);
        }
    }
}
