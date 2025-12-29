package com._glab.booking_system.auth.exception;

import lombok.experimental.StandardException;

/**
 * Thrown when MFA operations are rate-limited (e.g., too many email OTP requests).
 */
@StandardException
public class MfaRateLimitedException extends RuntimeException {}

