package com._glab.booking_system.auth.exception_handler;

import com._glab.booking_system.ErrorResponse;
import com._glab.booking_system.ErrorResponseCode;
import com._glab.booking_system.auth.exception.*;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Slf4j
public class AuthExceptionHandler {

    // ==================== JWT/Token Exceptions ====================

    @ExceptionHandler(InvalidJwtException.class)
    public ResponseEntity<ErrorResponse> handleInvalidJwt(InvalidJwtException e) {
        log.warn("Invalid JWT: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.AUTH_INVALID_TOKEN, e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(ExpiredJwtTokenException.class)
    public ResponseEntity<ErrorResponse> handleExpiredJwt(ExpiredJwtTokenException e) {
        log.debug("Expired JWT: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.AUTH_EXPIRED_TOKEN, e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    // ==================== Authentication Exceptions ====================

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ErrorResponse> handleAuthFailed(AuthenticationFailedException e) {
        log.warn("Authentication failed: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.AUTH_INVALID_CREDENTIALS, e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(AccountDisabledException.class)
    public ResponseEntity<ErrorResponse> handleAccountDisabled(AccountDisabledException e) {
        log.warn("Account disabled: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.AUTH_ACCOUNT_DISABLED, e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ErrorResponse> handleAccountLocked(AccountLockedException e) {
        log.warn("Account locked: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.AUTH_ACCOUNT_LOCKED, e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    // ==================== Refresh Token Exceptions ====================

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRefresh(InvalidRefreshTokenException e) {
        log.warn("Invalid refresh token: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.AUTH_INVALID_REFRESH_TOKEN, e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(RefreshTokenExpiredException.class)
    public ResponseEntity<ErrorResponse> handleExpiredRefresh(RefreshTokenExpiredException e) {
        log.debug("Expired refresh token: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.AUTH_REFRESH_TOKEN_EXPIRED, e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(RefreshTokenReuseException.class)
    public ResponseEntity<ErrorResponse> handleRefreshReuse(RefreshTokenReuseException e) {
        log.error("SECURITY: Refresh token reuse detected: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.AUTH_REFRESH_TOKEN_REUSE_DETECTED, e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    // ==================== Password Setup Token Exceptions ====================

    @ExceptionHandler(InvalidPasswordSetupTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPasswordToken(InvalidPasswordSetupTokenException e) {
        log.warn("Invalid password setup token: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.AUTH_PASSWORD_TOKEN_INVALID, e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(ExpiredPasswordSetupTokenException.class)
    public ResponseEntity<ErrorResponse> handleExpiredPasswordToken(ExpiredPasswordSetupTokenException e) {
        log.warn("Expired password setup token: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.AUTH_PASSWORD_TOKEN_EXPIRED, e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    // ==================== MFA Exceptions ====================

    @ExceptionHandler(MfaRequiredException.class)
    public ResponseEntity<ErrorResponse> handleMfaRequired(MfaRequiredException e) {
        log.warn("MFA required: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.AUTH_MFA_REQUIRED, e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(MfaSetupRequiredException.class)
    public ResponseEntity<ErrorResponse> handleMfaSetupRequired(MfaSetupRequiredException e) {
        log.info("MFA setup required: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.AUTH_MFA_SETUP_REQUIRED, e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(InvalidMfaCodeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidMfaCode(InvalidMfaCodeException e) {
        log.warn("Invalid MFA code: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.AUTH_MFA_INVALID_CODE, e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(InvalidMfaTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidMfaToken(InvalidMfaTokenException e) {
        log.warn("Invalid MFA token: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.AUTH_MFA_TOKEN_INVALID, e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(MfaTokenExpiredException.class)
    public ResponseEntity<ErrorResponse> handleMfaTokenExpired(MfaTokenExpiredException e) {
        log.debug("MFA token expired: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.AUTH_MFA_TOKEN_EXPIRED, e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(MfaRateLimitedException.class)
    public ResponseEntity<ErrorResponse> handleMfaRateLimited(MfaRateLimitedException e) {
        log.warn("MFA rate limited: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.AUTH_MFA_RATE_LIMITED, e.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
    }

    @ExceptionHandler(MfaAlreadyEnabledException.class)
    public ResponseEntity<ErrorResponse> handleMfaAlreadyEnabled(MfaAlreadyEnabledException e) {
        log.warn("MFA already enabled: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.AUTH_MFA_ALREADY_ENABLED, e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(MfaNotEnabledException.class)
    public ResponseEntity<ErrorResponse> handleMfaNotEnabled(MfaNotEnabledException e) {
        log.warn("MFA not enabled: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.AUTH_MFA_NOT_ENABLED, e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(MfaVerificationFailedException.class)
    public ResponseEntity<ErrorResponse> handleMfaVerificationFailed(MfaVerificationFailedException e) {
        log.warn("MFA verification failed: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.AUTH_MFA_INVALID_CODE, e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }
}



