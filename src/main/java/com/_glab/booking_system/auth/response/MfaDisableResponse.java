package com._glab.booking_system.auth.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response for MFA disable operation.
 */
@Schema(description = "Response for MFA disable operation")
public record MfaDisableResponse(
    @Schema(description = "Whether MFA is enabled after the operation", example = "false")
    boolean mfaEnabled,
    
    @Schema(description = "Status message", example = "MFA has been disabled")
    String message
) {}
