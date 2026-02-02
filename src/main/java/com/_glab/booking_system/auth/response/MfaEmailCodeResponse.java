package com._glab.booking_system.auth.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response for email OTP code request.
 */
@Schema(description = "Response for email verification code request")
public record MfaEmailCodeResponse(
    @Schema(description = "Whether the email was sent successfully", example = "true")
    boolean sent,
    
    @Schema(description = "Status message", example = "Verification code sent to your email")
    String message
) {}
