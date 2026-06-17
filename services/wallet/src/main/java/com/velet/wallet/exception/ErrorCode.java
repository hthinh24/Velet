package com.velet.wallet.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    USER_NOT_EXISTED(404, "User not existed", HttpStatus.NOT_FOUND),
    USERNAME_ALREADY_EXISTS(400, "Username already exists", HttpStatus.BAD_REQUEST),

    INVALID_CREDENTIALS(401, "Invalid credentials", HttpStatus.UNAUTHORIZED),
    INTERNAL_SERVER_ERROR(500, "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),

    INVALID_REQUEST(400, "Invalid request", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(403, "Unauthorized", HttpStatus.FORBIDDEN),
    UNAUTHORIZED_ACTION(403, "Unauthorized, you don`t have permission to do this action", HttpStatus.FORBIDDEN),
    INVALID_REFRESH_TOKEN(401, "Invalid refresh token", HttpStatus.UNAUTHORIZED),

    // Wallet domain
    WALLET_NOT_FOUND(404, "Wallet not found", HttpStatus.NOT_FOUND),
    WALLET_INACTIVE(422, "Wallet is inactive or suspended", HttpStatus.UNPROCESSABLE_ENTITY),
    INSUFFICIENT_FUNDS(422, "Insufficient funds", HttpStatus.UNPROCESSABLE_ENTITY),
    TRANSFER_TO_SELF(400, "Cannot transfer to the same wallet", HttpStatus.BAD_REQUEST),
    DUPLICATE_TRANSFER(409, "Duplicate transfer request", HttpStatus.CONFLICT),
    LOCK_ACQUISITION_FAILED(503, "Failed to acquire lock", HttpStatus.SERVICE_UNAVAILABLE),
    ;

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}

