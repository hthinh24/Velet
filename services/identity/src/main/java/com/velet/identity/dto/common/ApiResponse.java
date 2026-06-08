package com.velet.identity.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;
}
