package com.velet.identity.dto.request;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class RefreshRequest {
    private String refreshToken;
}
