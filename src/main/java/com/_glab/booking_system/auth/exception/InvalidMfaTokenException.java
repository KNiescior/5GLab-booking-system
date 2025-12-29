package com._glab.booking_system.auth.exception;

import lombok.experimental.StandardException;

/**
 * Thrown when an MFA token (intermediate JWT after password verification) is invalid.
 */
@StandardException
public class InvalidMfaTokenException extends RuntimeException {}

