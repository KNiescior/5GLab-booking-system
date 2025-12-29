package com._glab.booking_system.auth.exception;

import lombok.experimental.StandardException;

/**
 * Thrown when attempting to set up MFA when it's already enabled.
 */
@StandardException
public class MfaAlreadyEnabledException extends RuntimeException {}

