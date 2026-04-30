package com.yourorg.eventdashboard.shared;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    public record ErrorResponse(int status, String code, String message, String requestId) {}

    // ─── JWT / Auth ───────────────────────────────────────────────────────────

    @ExceptionHandler(JwtAuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleJwtAuthentication(JwtAuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(401, "UNAUTHORIZED", ex.getMessage(), MDC.get("requestId")));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(401, "UNAUTHORIZED", "Invalid credentials.", MDC.get("requestId")));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(403, "FORBIDDEN", "Access denied.", MDC.get("requestId")));
    }

    // ─── Domain — Event ───────────────────────────────────────────────────────

    @ExceptionHandler(EventNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEventNotFound(EventNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(404, "EVENT_NOT_FOUND", ex.getMessage(), MDC.get("requestId")));
    }

    @ExceptionHandler(EventCancelledException.class)
    public ResponseEntity<ErrorResponse> handleEventCancelled(EventCancelledException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse(422, "EVENT_CANCELLED", ex.getMessage(), MDC.get("requestId")));
    }

    @ExceptionHandler(EventInPastException.class)
    public ResponseEntity<ErrorResponse> handleEventInPast(EventInPastException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse(422, "EVENT_IN_PAST", ex.getMessage(), MDC.get("requestId")));
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCategoryNotFound(CategoryNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(404, "CATEGORY_NOT_FOUND", ex.getMessage(), MDC.get("requestId")));
    }

    @ExceptionHandler(InvalidEventTimeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidEventTime(InvalidEventTimeException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(400, "INVALID_EVENT_TIME", ex.getMessage(), MDC.get("requestId")));
    }

    // ─── Domain — Subscription ────────────────────────────────────────────────

    @ExceptionHandler(SubscriptionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSubscriptionNotFound(SubscriptionNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(404, "SUBSCRIPTION_NOT_FOUND", ex.getMessage(), MDC.get("requestId")));
    }

    @ExceptionHandler(AlreadySubscribedException.class)
    public ResponseEntity<ErrorResponse> handleAlreadySubscribed(AlreadySubscribedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(409, "ALREADY_SUBSCRIBED", ex.getMessage(), MDC.get("requestId")));
    }

    @ExceptionHandler(AlreadyOptedOutException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyOptedOut(AlreadyOptedOutException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(409, "ALREADY_OPTED_OUT", ex.getMessage(), MDC.get("requestId")));
    }

    @ExceptionHandler(InvalidPhoneNumberException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPhoneNumber(InvalidPhoneNumberException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(400, "INVALID_PHONE_NUMBER", ex.getMessage(), MDC.get("requestId")));
    }

    // ─── Domain — SMS ─────────────────────────────────────────────────────────

    @ExceptionHandler(SmsDeliveryException.class)
    public ResponseEntity<ErrorResponse> handleSmsDelivery(SmsDeliveryException ex) {
        log.warn("SMS delivery failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse(502, "SMS_DELIVERY_FAILED", "SMS delivery failed.", MDC.get("requestId")));
    }

    // ─── Infrastructure ───────────────────────────────────────────────────────

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(409, "CONFLICT", "A duplicate record was detected.", MDC.get("requestId")));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResourceFound(NoResourceFoundException ex) {
        return ResponseEntity.notFound().build();
    }

    // ─── Validation ───────────────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(400, "INVALID_REQUEST", message, MDC.get("requestId")));
    }

    // ─── Rate limiting (stub — implemented in Story 10) ──────────────────────
    // TODO: @ExceptionHandler(RateLimitedException.class) → 429, RATE_LIMITED

    // ─── Catch-all ────────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse(500, "INTERNAL_ERROR", "An unexpected error occurred.", MDC.get("requestId")));
    }
}
