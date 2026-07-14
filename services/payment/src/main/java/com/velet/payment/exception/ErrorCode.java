package com.velet.payment.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    INTERNAL_SERVER_ERROR(500, "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),

    INSUFFICIENT_BALANCE(422, "Insufficient balance", HttpStatus.UNPROCESSABLE_ENTITY),
    PAYMENT_NOT_FOUND(404, "Payment not found", HttpStatus.NOT_FOUND),
    INVALID_REQUEST(400, "Invalid request", HttpStatus.BAD_REQUEST),

    WALLET_SERVICE_UNAVAILABLE(503, "Wallet service unavailable, please try again later",
                               HttpStatus.SERVICE_UNAVAILABLE),
    MERCHANT_SERVICE_UNAVAILABLE(
            503, "Merchant service unavailable, please try again later", HttpStatus.SERVICE_UNAVAILABLE);


    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}
