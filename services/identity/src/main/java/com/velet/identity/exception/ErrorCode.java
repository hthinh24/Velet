package com.velet.identity.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    USER_NOT_EXISTED(404, "User not existed", HttpStatus.NOT_FOUND),
    USERNAME_ALREADY_EXISTS(400, "Username already exists", HttpStatus.BAD_REQUEST),

    INVALID_CREDENTIALS(401, "Invalid credentials", HttpStatus.UNAUTHORIZED),
    INTERNAL_SERVER_ERROR(500, "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),

    CHANNEL_NOT_FOUND(404, "Channel not found", HttpStatus.NOT_FOUND),
    SERVER_NOT_FOUND(404, "Server not found", HttpStatus.NOT_FOUND),
    CHANNEL_NOT_IN_SERVER(403, "Channel not in server", HttpStatus.FORBIDDEN),
    MEMBER_NOT_IN_SERVER(403, "Member not in server", HttpStatus.FORBIDDEN),
    RESOURCE_NOT_FOUND(404, "Resource not found", HttpStatus.NOT_FOUND),

    INVALID_REQUEST(400, "Invalid request", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(403, "Unauthorized", HttpStatus.FORBIDDEN),
    UNAUTHORIZED_ACTION(403, "Unauthorized action", HttpStatus.FORBIDDEN),
    INVALID_REFRESH_TOKEN(401, "Invalid refresh token", HttpStatus.UNAUTHORIZED),

    MESSAGE_NOT_FOUND(404, "Message not found", HttpStatus.NOT_FOUND),

    FILE_EMPTY(400, "File is empty", HttpStatus.BAD_REQUEST),
    UN_SUPPORTED_FILE_TYPE(415, "Unsupported file type", HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    ;

    private int code;
    private String message;
    private HttpStatusCode statusCode;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

}
