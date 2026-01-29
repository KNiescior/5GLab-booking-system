package com._glab.booking_system.user.exception;

/**
 * Thrown when a user is not found by ID.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String message) {
        super(message);
    }

    public UserNotFoundException(Integer id) {
        super("User not found with id: " + id);
    }
}
