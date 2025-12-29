package com._glab.booking_system.auth.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request to verify MFA setup by providing the first TOTP code.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MfaSetupVerifyRequest {
    /**
     * The temporary secret from the setup response.
     */
    private String secret;
    
    /**
     * The 6-digit TOTP code from the authenticator app.
     */
    private String code;
}

