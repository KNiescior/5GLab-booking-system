package com._glab.booking_system.auth.service;

import com._glab.booking_system.auth.config.JwtKeyProvider;
import com._glab.booking_system.auth.config.JwtProperties;
import com._glab.booking_system.user.model.Role;
import com._glab.booking_system.user.model.RoleName;
import com._glab.booking_system.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MfaServiceTest {

    @Mock
    private JwtKeyProvider keyProvider;

    @Mock
    private JwtProperties jwtProperties;

    private PasswordEncoder passwordEncoder;
    private MfaService mfaService;
    private KeyPair keyPair;

    private User adminUser;
    private User labManagerUser;
    private User professorUser;

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        passwordEncoder = new BCryptPasswordEncoder();

        // Generate test keys
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();

        mfaService = new MfaService(keyProvider, jwtProperties, passwordEncoder);

        // Create test users with different roles
        Role adminRole = new Role();
        adminRole.setName(RoleName.ADMIN);
        adminUser = new User();
        adminUser.setId(1);
        adminUser.setEmail("admin@example.com");
        adminUser.setRole(adminRole);

        Role labManagerRole = new Role();
        labManagerRole.setName(RoleName.LAB_MANAGER);
        labManagerUser = new User();
        labManagerUser.setId(2);
        labManagerUser.setEmail("labmanager@example.com");
        labManagerUser.setRole(labManagerRole);

        Role professorRole = new Role();
        professorRole.setName(RoleName.PROFESSOR);
        professorUser = new User();
        professorUser.setId(3);
        professorUser.setEmail("professor@example.com");
        professorUser.setRole(professorRole);
    }

    @Nested
    @DisplayName("Role Enforcement Tests")
    class RoleEnforcementTests {

        @Test
        @DisplayName("MFA should be required for Admin role")
        void mfaShouldBeRequiredForAdmin() {
            assertThat(mfaService.isMfaRequired(adminUser)).isTrue();
        }

        @Test
        @DisplayName("MFA should be required for Lab Manager role")
        void mfaShouldBeRequiredForLabManager() {
            assertThat(mfaService.isMfaRequired(labManagerUser)).isTrue();
        }

        @Test
        @DisplayName("MFA should NOT be required for Professor role")
        void mfaShouldNotBeRequiredForProfessor() {
            assertThat(mfaService.isMfaRequired(professorUser)).isFalse();
        }

        @Test
        @DisplayName("MFA should NOT be required for user without role")
        void mfaShouldNotBeRequiredForUserWithoutRole() {
            User noRoleUser = new User();
            noRoleUser.setEmail("norole@example.com");
            noRoleUser.setRole(null);

            assertThat(mfaService.isMfaRequired(noRoleUser)).isFalse();
        }

        @Test
        @DisplayName("needsMfaSetup should return true for Admin without MFA")
        void needsMfaSetupForAdminWithoutMfa() {
            adminUser.setMfaEnabled(false);
            assertThat(mfaService.needsMfaSetup(adminUser)).isTrue();
        }

        @Test
        @DisplayName("needsMfaSetup should return false for Admin with MFA enabled")
        void needsMfaSetupForAdminWithMfa() {
            adminUser.setMfaEnabled(true);
            assertThat(mfaService.needsMfaSetup(adminUser)).isFalse();
        }

        @Test
        @DisplayName("needsMfaSetup should return false for Professor without MFA")
        void needsMfaSetupForProfessorWithoutMfa() {
            professorUser.setMfaEnabled(false);
            assertThat(mfaService.needsMfaSetup(professorUser)).isFalse();
        }
    }

    @Nested
    @DisplayName("TOTP Tests")
    class TotpTests {

        @Test
        @DisplayName("Should generate a valid secret")
        void shouldGenerateValidSecret() {
            String secret = mfaService.generateSecret();

            assertThat(secret).isNotNull();
            assertThat(secret).isNotBlank();
            assertThat(secret.length()).isGreaterThanOrEqualTo(16);
        }

        @Test
        @DisplayName("Should generate QR code data URI")
        void shouldGenerateQrCodeDataUri() {
            String secret = mfaService.generateSecret();
            String qrCode = mfaService.generateQrCodeDataUri(professorUser, secret);

            assertThat(qrCode).isNotNull();
            assertThat(qrCode).startsWith("data:image/png;base64,");
        }

        @Test
        @DisplayName("Should generate OTP auth URI")
        void shouldGenerateOtpAuthUri() {
            String secret = "TESTSECRET123456";
            String uri = mfaService.generateOtpAuthUri(professorUser, secret);

            assertThat(uri).isNotNull();
            assertThat(uri).startsWith("otpauth://totp/");
            assertThat(uri).contains(professorUser.getEmail());
            assertThat(uri).contains("secret=" + secret);
        }

        @Test
        @DisplayName("Should return false for null secret in TOTP verification")
        void shouldReturnFalseForNullSecret() {
            assertThat(mfaService.verifyTotp(null, "123456")).isFalse();
        }

        @Test
        @DisplayName("Should return false for null code in TOTP verification")
        void shouldReturnFalseForNullCode() {
            assertThat(mfaService.verifyTotp("TESTSECRET", null)).isFalse();
        }
    }

    @Nested
    @DisplayName("Backup Code Tests")
    class BackupCodeTests {

        @Test
        @DisplayName("Should generate 10 backup codes")
        void shouldGenerate10BackupCodes() {
            MfaService.BackupCodesResult result = mfaService.generateBackupCodes();

            assertThat(result.plainCodes()).hasSize(10);
            assertThat(result.hashedCodes()).hasSize(10);
        }

        @Test
        @DisplayName("Backup codes should be unique")
        void backupCodesShouldBeUnique() {
            MfaService.BackupCodesResult result = mfaService.generateBackupCodes();

            long uniqueCount = result.plainCodes().stream().distinct().count();
            assertThat(uniqueCount).isEqualTo(10);
        }

        @Test
        @DisplayName("Backup codes should contain dash separator")
        void backupCodesShouldContainDash() {
            MfaService.BackupCodesResult result = mfaService.generateBackupCodes();

            for (String code : result.plainCodes()) {
                assertThat(code).contains("-");
            }
        }

        @Test
        @DisplayName("Should verify correct backup code")
        void shouldVerifyCorrectBackupCode() {
            MfaService.BackupCodesResult result = mfaService.generateBackupCodes();

            String codeToVerify = result.plainCodes().get(0);
            int index = mfaService.verifyBackupCode(codeToVerify, result.hashedCodes());

            assertThat(index).isEqualTo(0);
        }

        @Test
        @DisplayName("Should verify backup code without dash")
        void shouldVerifyBackupCodeWithoutDash() {
            MfaService.BackupCodesResult result = mfaService.generateBackupCodes();

            String codeWithDash = result.plainCodes().get(0);
            String codeWithoutDash = codeWithDash.replace("-", "");
            int index = mfaService.verifyBackupCode(codeWithoutDash, result.hashedCodes());

            assertThat(index).isEqualTo(0);
        }

        @Test
        @DisplayName("Should verify backup code case-insensitive")
        void shouldVerifyBackupCodeCaseInsensitive() {
            MfaService.BackupCodesResult result = mfaService.generateBackupCodes();

            String code = result.plainCodes().get(0).toLowerCase();
            int index = mfaService.verifyBackupCode(code, result.hashedCodes());

            assertThat(index).isEqualTo(0);
        }

        @Test
        @DisplayName("Should return -1 for invalid backup code")
        void shouldReturnNegativeForInvalidBackupCode() {
            MfaService.BackupCodesResult result = mfaService.generateBackupCodes();

            int index = mfaService.verifyBackupCode("INVALID-CODE", result.hashedCodes());

            assertThat(index).isEqualTo(-1);
        }

        @Test
        @DisplayName("Should return -1 for null code")
        void shouldReturnNegativeForNullCode() {
            MfaService.BackupCodesResult result = mfaService.generateBackupCodes();

            int index = mfaService.verifyBackupCode(null, result.hashedCodes());

            assertThat(index).isEqualTo(-1);
        }

        @Test
        @DisplayName("Should return -1 for null hashed codes")
        void shouldReturnNegativeForNullHashedCodes() {
            int index = mfaService.verifyBackupCode("SOME-CODE", null);

            assertThat(index).isEqualTo(-1);
        }
    }

    @Nested
    @DisplayName("MFA Token Tests")
    class MfaTokenTests {

        @Test
        @DisplayName("Should generate MFA token")
        void shouldGenerateMfaToken() {
            when(keyProvider.getPrivateKey()).thenReturn(keyPair.getPrivate());

            String token = mfaService.generateMfaToken(professorUser);

            assertThat(token).isNotNull();
            assertThat(token).isNotBlank();
            // JWT has 3 parts separated by dots
            assertThat(token.split("\\.")).hasSize(3);
        }

        @Test
        @DisplayName("Should parse valid MFA token")
        void shouldParseValidMfaToken() {
            when(keyProvider.getPrivateKey()).thenReturn(keyPair.getPrivate());
            when(keyProvider.getPublicKey()).thenReturn(keyPair.getPublic());

            String token = mfaService.generateMfaToken(professorUser);

            MfaService.MfaTokenClaims claims = mfaService.parseMfaToken(token);

            assertThat(claims.email()).isEqualTo(professorUser.getEmail());
            assertThat(claims.userId()).isEqualTo(professorUser.getId());
        }

        @Test
        @DisplayName("Should throw exception for invalid MFA token")
        void shouldThrowForInvalidMfaToken() {
            when(keyProvider.getPublicKey()).thenReturn(keyPair.getPublic());

            assertThatThrownBy(() -> mfaService.parseMfaToken("invalid.token.here"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Invalid MFA token");
        }

        @Test
        @DisplayName("Should throw exception for tampered MFA token")
        void shouldThrowForTamperedMfaToken() {
            when(keyProvider.getPrivateKey()).thenReturn(keyPair.getPrivate());
            when(keyProvider.getPublicKey()).thenReturn(keyPair.getPublic());

            String token = mfaService.generateMfaToken(professorUser);
            String tamperedToken = token.substring(0, token.length() - 5) + "xxxxx";

            assertThatThrownBy(() -> mfaService.parseMfaToken(tamperedToken))
                    .isInstanceOf(RuntimeException.class);
        }
    }
}

