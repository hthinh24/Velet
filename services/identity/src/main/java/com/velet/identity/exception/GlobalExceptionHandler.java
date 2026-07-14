package com.velet.identity.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.velet.identity.dto.common.ErrorResponse;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(value = AppException.class)
    ResponseEntity<ErrorResponse> handlingAppException(AppException exception) {
        log.error(exception.getMessage(), exception);
        ErrorCode errorCode = exception.getErrorCode();

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setCode(errorCode.getCode());
        errorResponse.setMessage(errorCode.getMessage());

        return ResponseEntity.status(errorCode.getStatusCode()).body(errorResponse);
    }

    @ExceptionHandler(value = Exception.class)
    ResponseEntity<ErrorResponse> handlingRuntimeException(Exception e) {
        log.error(e.getMessage(), e);
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setMessage(e.getMessage());
        errorResponse.setCode(400);
        return ResponseEntity.badRequest().body(errorResponse);
    }
}
