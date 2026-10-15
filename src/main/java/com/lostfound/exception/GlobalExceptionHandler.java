
package com.lostfound.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

        private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<Map<String, Object>> handleValidation(
                        MethodArgumentNotValidException exception) {

                Map<String, String> errors = new LinkedHashMap<>();

                for (FieldError error : exception.getBindingResult().getFieldErrors()) {
                        errors.putIfAbsent(
                                        error.getField(),
                                        error.getDefaultMessage() == null
                                                        ? "Invalid value"
                                                        : error.getDefaultMessage());
                }

                return buildResponse(
                                HttpStatus.BAD_REQUEST, "Validation failed", errors);
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<Map<String, Object>> handleAccessDenied(
                        AccessDeniedException exception) {

                return buildResponse(
                                HttpStatus.FORBIDDEN,
                                "You are not authorized to perform this action",
                                null);
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<Map<String, Object>> handleIllegalArgument(
                        IllegalArgumentException exception) {

                String message = exception.getMessage() == null
                                ? "Invalid request"
                                : exception.getMessage();

                HttpStatus status = HttpStatus.BAD_REQUEST;
                String lower = message.toLowerCase();

                if (lower.contains("not found")) {
                        status = HttpStatus.NOT_FOUND;
                } else if (lower.contains("only your own")
                                || lower.contains("not authorized")) {
                        status = HttpStatus.FORBIDDEN;
                }

                return buildResponse(status, message, null);
        }

        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<Map<String, Object>> handleBadCredentials(
                        BadCredentialsException exception) {

                return buildResponse(
                                HttpStatus.UNAUTHORIZED,
                                "Invalid email or password",
                                null);
        }

        @ExceptionHandler(DataIntegrityViolationException.class)
        public ResponseEntity<Map<String, Object>> handleDataIntegrity(
                        DataIntegrityViolationException exception) {

                return buildResponse(
                                HttpStatus.CONFLICT,
                                "The operation conflicts with existing data",
                                null);
        }

        @ExceptionHandler(MethodArgumentTypeMismatchException.class)
        public ResponseEntity<Map<String, Object>> handleTypeMismatch(
                        MethodArgumentTypeMismatchException exception) {

                return buildResponse(
                                HttpStatus.BAD_REQUEST,
                                "Invalid value for parameter: " + exception.getName(),
                                null);
        }

        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<Map<String, Object>> handleUnreadableRequest(
                        HttpMessageNotReadableException exception) {

                return buildResponse(
                                HttpStatus.BAD_REQUEST,
                                "Request body is invalid or malformed",
                                null);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<Map<String, Object>> handleGeneral(
                        Exception exception) {

                log.error("Unhandled application exception", exception);

                return buildResponse(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "An unexpected server error occurred",
                                null);
        }

        private ResponseEntity<Map<String, Object>> buildResponse(
                        HttpStatus status,
                        String message,
                        Object details) {

                Map<String, Object> body = new LinkedHashMap<>();
                body.put("timestamp", LocalDateTime.now());
                body.put("status", status.value());
                body.put("error", status.getReasonPhrase());
                body.put("message", message);

                if (details != null) {
                        body.put("details", details);
                }

                return ResponseEntity.status(status).body(body);
        }
}
