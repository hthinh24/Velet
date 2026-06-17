package com.velet.wallet.dto.common;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;
}
