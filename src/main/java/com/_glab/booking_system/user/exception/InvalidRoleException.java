package com._glab.booking_system.user.exception;

import lombok.experimental.StandardException;

/**
 * Thrown when an invalid role is specified during user creation.
 */
@StandardException
public class InvalidRoleException extends RuntimeException {
}
