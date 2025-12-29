package com._glab.booking_system.auth.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request to disable MFA (requires current TOTP code for security).
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MfaDisableRequest {
    /**
     * Current TOTP code to verify identity before disabling MFA.
     */
    private String code;
}

