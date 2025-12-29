package com._glab.booking_system.auth.service;

import com._glab.booking_system.auth.exception.ExpiredPasswordSetupTokenException;
import com._glab.booking_system.auth.exception.InvalidPasswordSetupTokenException;
import com._glab.booking_system.auth.model.PasswordSetupToken;
import com._glab.booking_system.auth.model.TokenPurpose;
import com._glab.booking_system.auth.repository.PasswordSetupTokenRepository;
import com._glab.booking_system.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordSetupTokenServiceTest {

    @Mock
    private PasswordSetupTokenRepository tokenRepository;

    @InjectMocks
    private PasswordSetupTokenService tokenService;

    private User testUser;
    private PasswordSetupToken validToken;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1);
        testUser.setEmail("test@example.com");

        validToken = new PasswordSetupToken();
        validToken.setId(UUID.randomUUID());
        validToken.setUser(testUser);
        validToken.setTokenHash("hashed-token");
        validToken.setPurpose(TokenPurpose.ACCOUNT_SETUP);
        validToken.setExpiresAt(OffsetDateTime.now().plusHours(24));
        validToken.setUsedAt(null);
    }

    @Nested
    @DisplayName("Create Token Tests")
    class CreateTokenTests {

        @Test
        @DisplayName("Should create new token and invalidate existing ones")
        void shouldCreateTokenAndInvalidateExisting() {
            // When
            String rawToken = tokenService.createToken(testUser, TokenPurpose.ACCOUNT_SETUP);

            // Then
            assertThat(rawToken).isNotBlank();
            assertThat(rawToken.length()).isGreaterThan(20); // Base64 encoded 32 bytes

            verify(tokenRepository).invalidateTokensForUser(eq(testUser), eq(TokenPurpose.ACCOUNT_SETUP), any(OffsetDateTime.class));
            
            ArgumentCaptor<PasswordSetupToken> tokenCaptor = ArgumentCaptor.forClass(PasswordSetupToken.class);
            verify(tokenRepository).save(tokenCaptor.capture());
            
            PasswordSetupToken savedToken = tokenCaptor.getValue();
            assertThat(savedToken.getUser()).isEqualTo(testUser);
            assertThat(savedToken.getPurpose()).isEqualTo(TokenPurpose.ACCOUNT_SETUP);
            assertThat(savedToken.getExpiresAt()).isAfter(OffsetDateTime.now().plusHours(47));
            assertThat(savedToken.getTokenHash()).isNotBlank();
        }

        @Test
        @DisplayName("Should generate unique tokens")
        void shouldGenerateUniqueTokens() {
            String token1 = tokenService.createToken(testUser, TokenPurpose.ACCOUNT_SETUP);
            String token2 = tokenService.createToken(testUser, TokenPurpose.ACCOUNT_SETUP);

            assertThat(token1).isNotEqualTo(token2);
        }
    }

    @Nested
    @DisplayName("Validate and Consume Token Tests")
    class ValidateAndConsumeTokenTests {

        @Test
        @DisplayName("Should consume valid token and return user")
        void shouldConsumeValidToken() {
            when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(validToken));

            User result = tokenService.validateAndConsumeToken("raw-token");

            assertThat(result).isEqualTo(testUser);
            assertThat(validToken.getUsedAt()).isNotNull();
            verify(tokenRepository).save(validToken);
        }

        @Test
        @DisplayName("Should throw InvalidPasswordSetupTokenException for unknown token")
        void shouldThrowForUnknownToken() {
            when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tokenService.validateAndConsumeToken("unknown-token"))
                    .isInstanceOf(InvalidPasswordSetupTokenException.class)
                    .hasMessage("Password setup token not found");
        }

        @Test
        @DisplayName("Should throw InvalidPasswordSetupTokenException for already used token")
        void shouldThrowForUsedToken() {
            validToken.setUsedAt(OffsetDateTime.now().minusHours(1));
            when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(validToken));

            assertThatThrownBy(() -> tokenService.validateAndConsumeToken("used-token"))
                    .isInstanceOf(InvalidPasswordSetupTokenException.class)
                    .hasMessage("Password setup token has already been used");
        }

        @Test
        @DisplayName("Should throw ExpiredPasswordSetupTokenException for expired token")
        void shouldThrowForExpiredToken() {
            validToken.setExpiresAt(OffsetDateTime.now().minusHours(1));
            when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(validToken));

            assertThatThrownBy(() -> tokenService.validateAndConsumeToken("expired-token"))
                    .isInstanceOf(ExpiredPasswordSetupTokenException.class)
                    .hasMessage("Password setup token has expired");
        }
    }

    @Nested
    @DisplayName("Cleanup Tests")
    class CleanupTests {

        @Test
        @DisplayName("Should delete expired tokens")
        void shouldDeleteExpiredTokens() {
            when(tokenRepository.deleteExpiredTokens(any(OffsetDateTime.class))).thenReturn(5);

            int deleted = tokenService.cleanupExpiredTokens();

            assertThat(deleted).isEqualTo(5);
            verify(tokenRepository).deleteExpiredTokens(any(OffsetDateTime.class));
        }
    }
}

