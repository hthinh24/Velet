package com.velet.identity.dto.request;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@RequiredArgsConstructor
@Getter
public class RegisterRequest {
    private final String email;
    private final String password;
    private final String confirmPassword;
    private final String displayName;
    private final LocalDate dateOfBirth;
}
