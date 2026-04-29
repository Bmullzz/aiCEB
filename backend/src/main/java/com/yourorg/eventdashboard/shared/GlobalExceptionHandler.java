package com.yourorg.eventdashboard.shared;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    public record ErrorResponse(int status, String code, String message, String requestId) {}

    // TODO: Add handler for EventNotFoundException        → 404, EVENT_NOT_FOUND
    // TODO: Add handler for EventCancelledException       → 422, EVENT_CANCELLED
    // TODO: Add handler for EventInPastException          → 422, EVENT_IN_PAST
    // TODO: Add handler for CategoryNotFoundException     → 404, CATEGORY_NOT_FOUND
    // TODO: Add handler for SubscriptionNotFoundException → 404, SUBSCRIPTION_NOT_FOUND
    // TODO: Add handler for AlreadySubscribedException    → 409, ALREADY_SUBSCRIBED
    // TODO: Add handler for AlreadyOptedOutException      → 409, ALREADY_OPTED_OUT
    // TODO: Add handler for InvalidPhoneNumberException   → 400, INVALID_PHONE_NUMBER

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(400, "INVALID_REQUEST", message, MDC.get("requestId")));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(409)
                .body(new ErrorResponse(409, "CONFLICT", "A duplicate record was detected.", MDC.get("requestId")));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse(500, "INTERNAL_ERROR", "An unexpected error occurred.", MDC.get("requestId")));
    }
}
