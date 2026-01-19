package com._glab.booking_system.auth.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Response indicating MFA setup is required before login can complete.
 * Returned when password is correct but user's role requires MFA and it's not yet set up.
 */
@Getter
@Setter
@AllArgsConstructor
public class MfaSetupRequiredResponse {
    /**
     * Short-lived token proving password was correct.
     * Use this token with /mfa/setup to begin MFA configuration.
     */
    private String mfaToken;
    
    /**
     * Indicates MFA setup is required.
     */
    private boolean mfaSetupRequired = true;
    
    /**
     * Message explaining next steps.
     */
    private String message = "MFA setup is required for your account. Use the mfaToken with /api/v1/mfa/setup to configure MFA.";
    
    public MfaSetupRequiredResponse(String mfaToken) {
        this.mfaToken = mfaToken;
    }
}
