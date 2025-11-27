package com._glab.booking_system.auth.exception_handler;

import com._glab.booking_system.ErrorResponse;
import com._glab.booking_system.ErrorResponseCode;
import com._glab.booking_system.auth.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(InvalidJwtException.class)
    public ResponseEntity<ErrorResponse> handleInvalidJwt(InvalidJwtException e) {
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.AUTH_INVALID_TOKEN, e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(ExpiredJwtTokenException.class)
    public ResponseEntity<ErrorResponse> handleExpiredJwt(ExpiredJwtTokenException e) {
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.AUTH_EXPIRED_TOKEN, e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ErrorResponse> handleAuthFailed(AuthenticationFailedException e) {
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.AUTH_INVALID_CREDENTIALS, e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(AccountDisabledException.class)
    public ResponseEntity<ErrorResponse> handleAccountDisabled(AccountDisabledException e) {
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.AUTH_ACCOUNT_DISABLED, e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ErrorResponse> handleAccountLocked(AccountLockedException e) {
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.AUTH_ACCOUNT_LOCKED, e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRefresh(InvalidRefreshTokenException e) {
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.AUTH_INVALID_REFRESH_TOKEN, e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(RefreshTokenExpiredException.class)
    public ResponseEntity<ErrorResponse> handleExpiredRefresh(RefreshTokenExpiredException e) {
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.AUTH_REFRESH_TOKEN_EXPIRED, e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(RefreshTokenReuseException.class)
    public ResponseEntity<ErrorResponse> handleRefreshReuse(RefreshTokenReuseException e) {
        // Also log as security event in the service layer when throwing this
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.AUTH_REFRESH_TOKEN_REUSE_DETECTED, e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(InvalidPasswordSetupTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPasswordToken(InvalidPasswordSetupTokenException e) {
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.AUTH_PASSWORD_TOKEN_INVALID, e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(ExpiredPasswordSetupTokenException.class)
    public ResponseEntity<ErrorResponse> handleExpiredPasswordToken(ExpiredPasswordSetupTokenException e) {
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.AUTH_PASSWORD_TOKEN_EXPIRED, e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
}



