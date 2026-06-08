package com.velet.identity.service;

import com.velet.identity.dto.request.LoginRequest;
import com.velet.identity.dto.request.LogoutRequest;
import com.velet.identity.dto.request.RefreshRequest;
import com.velet.identity.dto.request.RegisterRequest;
import com.velet.identity.dto.response.AuthenticationResponse;

public interface AuthenticationService {
    AuthenticationResponse login(LoginRequest loginRequest);

    AuthenticationResponse register(RegisterRequest registerRequest);

    AuthenticationResponse refreshToken(RefreshRequest refreshRequest);

    Boolean logout(LogoutRequest logoutRequest);
}
