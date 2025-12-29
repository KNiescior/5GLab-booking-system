package com._glab.booking_system.auth.exception;

import lombok.experimental.StandardException;

/**
 * Thrown when MFA code verification fails (wrong code).
 */
@StandardException
public class MfaVerificationFailedException extends RuntimeException {}

