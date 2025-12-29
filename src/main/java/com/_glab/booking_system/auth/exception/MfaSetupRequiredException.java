package com._glab.booking_system.auth.exception;

import lombok.experimental.StandardException;

/**
 * Thrown when a user (Admin/Lab Manager) must set up MFA before proceeding.
 */
@StandardException
public class MfaSetupRequiredException extends RuntimeException {}

