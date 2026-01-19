package com._glab.booking_system.booking.exception_handler;

import com._glab.booking_system.ErrorResponse;
import com._glab.booking_system.ErrorResponseCode;
import com._glab.booking_system.booking.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class BookingExceptionHandler {

    // ==================== Resource Not Found ====================

    @ExceptionHandler(LabNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleLabNotFound(LabNotFoundException e) {
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.BOOKING_LAB_NOT_FOUND, e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(WorkstationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleWorkstationNotFound(WorkstationNotFoundException e) {
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.BOOKING_WORKSTATION_NOT_FOUND, e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(ReservationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleReservationNotFound(ReservationNotFoundException e) {
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.BOOKING_RESERVATION_NOT_FOUND, e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(BuildingNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBuildingNotFound(BuildingNotFoundException e) {
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.BOOKING_BUILDING_NOT_FOUND, e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    // ==================== Validation Errors ====================

    @ExceptionHandler(InvalidReservationTimeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTime(InvalidReservationTimeException e) {
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.BOOKING_INVALID_TIME_RANGE, e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(OutsideOperatingHoursException.class)
    public ResponseEntity<ErrorResponse> handleOutsideOperatingHours(OutsideOperatingHoursException e) {
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.BOOKING_OUTSIDE_OPERATING_HOURS, e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(LabClosedException.class)
    public ResponseEntity<ErrorResponse> handleLabClosed(LabClosedException e) {
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.BOOKING_LAB_CLOSED, e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(WorkstationNotInLabException.class)
    public ResponseEntity<ErrorResponse> handleWorkstationNotInLab(WorkstationNotInLabException e) {
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.BOOKING_WORKSTATION_NOT_IN_LAB, e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(WorkstationInactiveException.class)
    public ResponseEntity<ErrorResponse> handleWorkstationInactive(WorkstationInactiveException e) {
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.BOOKING_WORKSTATION_INACTIVE, e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(NoWorkstationsSelectedException.class)
    public ResponseEntity<ErrorResponse> handleNoWorkstationsSelected(NoWorkstationsSelectedException e) {
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.BOOKING_NO_WORKSTATIONS_SELECTED, e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    // ==================== Recurring Errors ====================

    @ExceptionHandler(InvalidRecurringPatternException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRecurringPattern(InvalidRecurringPatternException e) {
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.BOOKING_INVALID_RECURRING_PATTERN, e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(NoValidOccurrencesException.class)
    public ResponseEntity<ErrorResponse> handleNoValidOccurrences(NoValidOccurrencesException e) {
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.BOOKING_NO_VALID_OCCURRENCES, e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    // ==================== Authorization ====================

    @ExceptionHandler(BookingNotAuthorizedException.class)
    public ResponseEntity<ErrorResponse> handleNotAuthorized(BookingNotAuthorizedException e) {
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.BOOKING_NOT_AUTHORIZED, e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }
}
