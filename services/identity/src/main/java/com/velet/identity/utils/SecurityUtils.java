package com.velet.identity.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import com.velet.identity.exception.AppException;
import com.velet.identity.exception.ErrorCode;

import java.security.Principal;

@Component
public class SecurityUtils {

    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        return Long.parseLong(authentication.getName());
    }

    public Long getCurrentlyUserIdThroughPrincipal(Principal principal) {
        if (principal == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        return Long.parseLong(principal.getName());
    }
}