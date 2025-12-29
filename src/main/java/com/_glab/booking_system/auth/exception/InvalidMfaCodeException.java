package com._glab.booking_system.auth.exception;

import lombok.experimental.StandardException;

/**
 * Thrown when an MFA verification code (TOTP, email OTP, or backup code) is invalid.
 */
@StandardException
public class InvalidMfaCodeException extends RuntimeException {}

