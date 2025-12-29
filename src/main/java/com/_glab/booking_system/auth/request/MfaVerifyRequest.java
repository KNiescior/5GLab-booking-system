package com._glab.booking_system.auth.request;

import com._glab.booking_system.auth.model.MfaCodeType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MfaVerifyRequest {
    private String mfaToken;
    private String code;
    
    /**
     * Type of code: TOTP, EMAIL, or BACKUP.
     * Defaults to TOTP if not specified.
     */
    private MfaCodeType codeType = MfaCodeType.TOTP;
}

