package com._glab.booking_system.booking.exception;

public class LabNotFoundException extends RuntimeException {
    public LabNotFoundException(String message) {
        super(message);
    }

    public LabNotFoundException(Integer labId) {
        super("Lab not found: " + labId);
    }
}
