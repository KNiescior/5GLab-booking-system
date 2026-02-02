package com._glab.booking_system.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for requesting an email OTP code as MFA fallback.
 */
@Data
@Schema(description = "Request to send email verification code for MFA")
public class MfaEmailCodeRequest {

    @NotBlank(message = "MFA token is required")
    @Schema(
        description = "MFA token received from login response",
        example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String mfaToken;
}
