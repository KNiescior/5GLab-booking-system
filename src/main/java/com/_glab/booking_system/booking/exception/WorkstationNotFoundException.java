package com._glab.booking_system.booking.exception;

public class WorkstationNotFoundException extends RuntimeException {
    public WorkstationNotFoundException(String message) {
        super(message);
    }

    public WorkstationNotFoundException(Integer workstationId) {
        super("Workstation not found: " + workstationId);
    }
}
