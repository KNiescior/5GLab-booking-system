package com._glab.booking_system.booking.exception;

public class NotLabManagerException extends RuntimeException {
    public NotLabManagerException(String message) {
        super(message);
    }

    public NotLabManagerException(Integer labId) {
        super("User is not a lab manager for lab: " + labId);
    }
}
