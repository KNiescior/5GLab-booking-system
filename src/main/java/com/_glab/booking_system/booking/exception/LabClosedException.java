package com._glab.booking_system.booking.exception;

import java.time.LocalDate;

public class LabClosedException extends RuntimeException {
    public LabClosedException(String message) {
        super(message);
    }

    public LabClosedException(String labName, LocalDate date) {
        super("Lab '" + labName + "' is closed on " + date);
    }
}
