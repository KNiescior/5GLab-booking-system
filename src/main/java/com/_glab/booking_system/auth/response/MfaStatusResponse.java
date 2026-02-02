package com._glab.booking_system.auth.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response for MFA status check.
 */
@Schema(description = "MFA status for the current user")
public record MfaStatusResponse(
    @Schema(description = "Whether MFA is currently enabled for the user", example = "true")
    boolean mfaEnabled,
    
    @Schema(description = "Whether MFA is required for this user's role", example = "true")
    boolean mfaRequired,
    
    @Schema(description = "Whether the user can disable MFA (false for Admin/Lab Manager roles)", example = "false")
    boolean canDisable
) {}
