package com.velet.identity.dto.request;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class LogoutRequest {
    private final String accessToken;
    private final String refreshToken;
}
