package com._glab.booking_system.auth.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Response after successfully completing MFA setup.
 * Contains backup codes that should be saved by the user.
 * If setup was done via mfaToken, also contains login credentials.
 */
@Getter
@Setter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
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
     * Access token (only provided when setup was via mfaToken - completes login).
     */
    private String accessToken;
    
    /**
     * User ID (only provided when setup was via mfaToken).
     */
    private Integer userId;
    
    /**
     * User email (only provided when setup was via mfaToken).
     */
    private String email;
    
    /**
     * User role (only provided when setup was via mfaToken).
     */
    private String role;
    
    /**
     * Message to display to the user.
     */
    private String message;
    
    /**
     * Constructor for regular MFA setup (user already logged in).
     */
    public MfaSetupCompleteResponse(boolean mfaEnabled, List<String> backupCodes, String message) {
        this.mfaEnabled = mfaEnabled;
        this.backupCodes = backupCodes;
        this.message = message;
    }
}

