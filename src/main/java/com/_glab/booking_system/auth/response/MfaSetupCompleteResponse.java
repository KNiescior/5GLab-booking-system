package com._glab.booking_system.auth.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Response after successfully completing MFA setup.
 * Contains backup codes that should be saved by the user.
 */
@Getter
@Setter
@AllArgsConstructor
public class MfaSetupCompleteResponse {
    /**
     * Whether MFA is now enabled.
     */
    private boolean mfaEnabled;
    
    /**
     * Backup codes for account recovery (show once, user must save them).
     */
    private List<String> backupCodes;
    
    /**
     * Message to display to the user.
     */
    private String message;
}

