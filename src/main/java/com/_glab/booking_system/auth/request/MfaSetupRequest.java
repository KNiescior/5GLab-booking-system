package com._glab.booking_system.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Request body for initiating MFA setup.
 * Used when starting MFA configuration for a user.
 */
@Data
@Schema(description = "Request to initiate MFA setup")
public class MfaSetupRequest {

    @Schema(
        description = "MFA token received from login response (for mandatory MFA setup before first login). " +
                      "Not required if already authenticated with JWT.",
        example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
    )
    private String mfaToken;
}
