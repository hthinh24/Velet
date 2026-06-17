package com.velet.wallet.exception;

import com.velet.wallet.dto.common.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException e) {
        ErrorCode ec = e.getErrorCode();
        log.warn("app.exception code={} message={}", ec.getCode(), ec.getMessage());

        return ResponseEntity.status(ec.getStatusCode())
                .body(ErrorResponse.builder()
                        .code(ec.getCode())
                        .message(ec.getMessage())
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.error("unexpected.exception class={}, message={}", e.getClass().getSimpleName(), e.getMessage(), e);

        return ResponseEntity.status(400)
                .body(ErrorResponse.builder()
                        .code(400)
                        .message(message)
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("unexpected.exception class={}, message={}", e.getClass().getSimpleName(), e.getMessage(), e);

        return ResponseEntity.status(500)
                .body(ErrorResponse.builder()
                        .code(500)
                        .message("Internal server error")
                        .timestamp(LocalDateTime.now())
                        .build());
    }
}
