package com._glab.booking_system.auth.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Response indicating MFA verification is required.
 * Returned when password is correct but MFA is enabled.
 */
@Getter
@Setter
@AllArgsConstructor
public class MfaChallengeResponse {
    /**
     * Short-lived token proving password was correct (5 minutes).
     */
    private String mfaToken;
    
    /**
     * Indicates MFA is required.
     */
    private boolean mfaRequired;
    
    /**
     * Available MFA methods: "totp", "email", "backup"
     */
    private String[] availableMethods;
    
    public MfaChallengeResponse(String mfaToken) {
        this.mfaToken = mfaToken;
        this.mfaRequired = true;
        this.availableMethods = new String[]{"totp", "email", "backup"};
    }
}

