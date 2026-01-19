package com._glab.booking_system.booking.exception;

public class WorkstationNotInLabException extends RuntimeException {
    public WorkstationNotInLabException(String message) {
        super(message);
    }

    public WorkstationNotInLabException(Integer workstationId, Integer labId) {
        super("Workstation " + workstationId + " does not belong to lab " + labId);
    }
}
