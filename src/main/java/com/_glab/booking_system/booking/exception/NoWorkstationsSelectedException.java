package com._glab.booking_system.booking.exception;

public class NoWorkstationsSelectedException extends RuntimeException {
    public NoWorkstationsSelectedException() {
        super("At least one workstation must be selected, or choose 'whole lab'");
    }

    public NoWorkstationsSelectedException(String message) {
        super(message);
    }
}
