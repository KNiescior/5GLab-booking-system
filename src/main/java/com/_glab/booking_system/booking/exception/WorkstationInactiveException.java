package com._glab.booking_system.booking.exception;

public class WorkstationInactiveException extends RuntimeException {
    public WorkstationInactiveException(String workstationIdentifier) {
        super("Workstation '" + workstationIdentifier + "' is not active");
    }
}
