package com._glab.booking_system;

public enum ErrorResponseCode {
    USER_EMAIL_NOT_VALID,

    // Authentication / JWT
    AUTH_INVALID_TOKEN,
    AUTH_EXPIRED_TOKEN,
    AUTH_INVALID_CREDENTIALS,
    AUTH_ACCOUNT_DISABLED,
    AUTH_ACCOUNT_LOCKED,

    // Refresh tokens
    AUTH_INVALID_REFRESH_TOKEN,
    AUTH_REFRESH_TOKEN_EXPIRED,
    AUTH_REFRESH_TOKEN_REUSE_DETECTED,

    // Password setup / reset tokens
    AUTH_PASSWORD_TOKEN_INVALID,
    AUTH_PASSWORD_TOKEN_EXPIRED
}
