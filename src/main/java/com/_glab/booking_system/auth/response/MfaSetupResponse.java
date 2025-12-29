package com._glab.booking_system.auth.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Response containing MFA setup information.
 */
@Getter
@Setter
@AllArgsConstructor
public class MfaSetupResponse {
    /**
     * The TOTP secret (to be stored temporarily by client until verified).
     */
    private String secret;
    
    /**
     * QR code as data URI (base64 PNG) for scanning with authenticator app.
     */
    private String qrCodeDataUri;
    
    /**
     * Manual entry URI (otpauth://) for authenticator apps.
     */
    private String manualEntryUri;
}

