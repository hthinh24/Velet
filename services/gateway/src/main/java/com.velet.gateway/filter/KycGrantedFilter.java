package com.velet.gateway.filter;

import com.velet.gateway.enums.KYCStatus;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@Order(2)
public class KycGrantedFilter extends OncePerRequestFilter {

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final List<String> SECURE_PATHS = List.of(
            "/api/v1/kyc/**",
            "/api/v1/wallets/**",
            "/api/v1/payments/**"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestPath = request.getRequestURI();

        boolean requiresKyc = SECURE_PATHS.stream()
                                       .anyMatch(path -> pathMatcher.match(path, requestPath));

        if (requiresKyc) {
            String kycStatus = request.getHeader("X-Kyc-Status");

            if (!KYCStatus.VERIFIED.toString().equals(kycStatus)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"ACCESS_DENIED\", \"message\": \"User must be KYC VERIFIED to access this resource.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}