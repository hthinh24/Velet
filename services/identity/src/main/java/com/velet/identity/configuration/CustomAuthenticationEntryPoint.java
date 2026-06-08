package com.velet.identity.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom AuthenticationEntryPoint to return JSON response for unauthorized
 * access.
 * Return 401 Unauthorized with a JSON body.
 */

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper(); // Jackson mapper

    @Override
    public void commence(HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        // 1. Set the Header
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        // 2. Build Error Body
        Map<String, Object> body = new HashMap<>();
        body.put("code", 401);
        body.put("status", "Unauthorized");
        body.put("message", authException.getMessage());
        body.put("path", request.getServletPath());

        // 3. Write to the response stream
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}