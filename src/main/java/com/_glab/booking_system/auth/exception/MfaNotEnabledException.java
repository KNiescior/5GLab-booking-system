package com._glab.booking_system.auth.exception;

import lombok.experimental.StandardException;

/**
 * Thrown when attempting to disable MFA when it's not enabled.
 */
@StandardException
public class MfaNotEnabledException extends RuntimeException {}

