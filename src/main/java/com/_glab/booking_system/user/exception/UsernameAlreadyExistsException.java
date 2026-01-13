package com._glab.booking_system.user.exception;

import lombok.experimental.StandardException;

/**
 * Thrown when attempting to create a user with a username that already exists.
 */
@StandardException
public class UsernameAlreadyExistsException extends RuntimeException {
}
