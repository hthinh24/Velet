package com.velet.wallet.dto.common;

import lombok.*;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class ErrorResponse {
    private int code;
    private String message;
    private LocalDateTime timestamp;
}
