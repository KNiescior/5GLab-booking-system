package com._glab.booking_system.auth.exception;

import lombok.experimental.StandardException;

/**
 * Thrown when MFA verification is required but not provided.
 */
@StandardException
public class MfaRequiredException extends RuntimeException {}

