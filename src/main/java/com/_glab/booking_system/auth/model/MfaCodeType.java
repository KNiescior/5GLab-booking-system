package com._glab.booking_system.auth.model;

/**
 * Type of MFA verification code.
 */
public enum MfaCodeType {
    /**
     * Time-based One-Time Password from authenticator app.
     */
    TOTP,
    
    /**
     * One-Time Password sent via email.
     */
    EMAIL,
    
    /**
     * Single-use backup/recovery code.
     */
    BACKUP
}

