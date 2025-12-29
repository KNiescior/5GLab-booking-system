package com._glab.booking_system.auth.exception;

import lombok.experimental.StandardException;

/**
 * Thrown when an MFA token has expired (5 minutes after password verification).
 */
@StandardException
public class MfaTokenExpiredException extends RuntimeException {}

