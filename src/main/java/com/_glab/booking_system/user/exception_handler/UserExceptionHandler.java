package com._glab.booking_system.user.exception_handler;

import com._glab.booking_system.ErrorResponse;
import com._glab.booking_system.ErrorResponseCode;
import com._glab.booking_system.user.exception.InvalidRoleException;
import com._glab.booking_system.user.exception.UserAlreadyExistsException;
import com._glab.booking_system.user.exception.UsernameAlreadyExistsException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Exception handler for user-related exceptions.
 */
@ControllerAdvice
@Slf4j
public class UserExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExistsException(UserAlreadyExistsException e) {
        log.warn("User registration failed - email already exists: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                ErrorResponseCode.USER_EMAIL_ALREADY_EXISTS,
                e.getMessage() != null ? e.getMessage() : "A user with this email already exists"
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUsernameAlreadyExistsException(UsernameAlreadyExistsException e) {
        log.warn("User registration failed - username already exists: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                ErrorResponseCode.USER_USERNAME_ALREADY_EXISTS,
                e.getMessage() != null ? e.getMessage() : "A user with this username already exists"
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(InvalidRoleException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRoleException(InvalidRoleException e) {
        log.warn("User registration failed - invalid role: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                ErrorResponseCode.USER_INVALID_ROLE,
                e.getMessage() != null ? e.getMessage() : "Invalid role specified"
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
}
