package com.efiscal.backend.exception;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        ErrorResponse error = new ErrorResponse(
            OffsetDateTime.now(ZoneOffset.UTC).toString(),
            status.value(),
            status.getReasonPhrase(),
            status.name(),
            ex.getReason(),
            UUID.randomUUID().toString(),
            Collections.emptyList()
        );
        return new ResponseEntity<>(error, status);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ErrorResponse error = new ErrorResponse(
            OffsetDateTime.now(ZoneOffset.UTC).toString(),
            status.value(),
            status.getReasonPhrase(),
            "INTERNAL_SERVER_ERROR",
            ex.getMessage(),
            UUID.randomUUID().toString(),
            Collections.emptyList()
        );
        return new ResponseEntity<>(error, status);
    }

    public record ErrorResponse(
        String timestamp,
        int status,
        String error,
        String code,
        String message,
        String correlationId,
        java.util.List<String> details
    ) {}
}
