package com.yourorg.eventdashboard.shared;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(400, "INVALID_REQUEST", message, MDC.get("requestId")));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(409, "ALREADY_SUBSCRIBED", "A duplicate record was detected.", MDC.get("requestId")));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse(500, "INTERNAL_ERROR", "An unexpected error occurred.", MDC.get("requestId")));
    }
}
