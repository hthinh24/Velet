package com.velet.identity.dto.common;

import lombok.*;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class ErrorResponse {
    private int code;
    private String message;
    private LocalDateTime timestamp;
}
