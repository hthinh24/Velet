package com.velet.identity.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.velet.identity.dto.request.LoginRequest;
import com.velet.identity.dto.request.LogoutRequest;
import com.velet.identity.dto.request.RefreshRequest;
import com.velet.identity.dto.request.RegisterRequest;
import com.velet.identity.dto.response.AuthenticationResponse;
import com.velet.identity.dto.response.UserBaseInfo;
import com.velet.identity.exception.AppException;
import com.velet.identity.exception.ErrorCode;
import com.velet.identity.models.Role;
import com.velet.identity.models.User;
import com.velet.identity.models.UserProfile;
import com.velet.identity.models.UserRole;
import com.velet.identity.repository.RoleRepository;
import com.velet.identity.repository.UserRepository;
import com.velet.identity.service.AuthenticationService;
import com.velet.identity.service.TokenBlacklistService;
import com.velet.identity.utils.JwtUtil;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationServiceImpl implements AuthenticationService {

    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final TokenBlacklistService tokenBlacklistService;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    public AuthenticationResponse login(LoginRequest loginRequest) {
        log.info("Login attempt for user: {}", loginRequest.getUsername());

        User user = userRepository.findByUsername(loginRequest.getUsername())
                                  .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            log.warn("Invalid credentials for user: {}", loginRequest.getUsername());
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        log.info("User {} logged in successfully", loginRequest.getUsername());
        return createAuthenticationResponse(user);
    }

    @Override
    @Transactional
    public AuthenticationResponse register(RegisterRequest registerRequest) {
        log.info("Register attempt for email: {}", registerRequest.getEmail());

        if (userRepository.existsByUsername(registerRequest.getEmail())) {
            log.warn("Username {} is already taken", registerRequest.getEmail());
            throw new AppException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        List<Role> roles = roleRepository.findUserDefaultRoles().orElse(new ArrayList<>());

        User newUser = User.builder()
                           .username(registerRequest.getEmail())
                           .password(passwordEncoder.encode(registerRequest.getPassword()))
                           .isActive(true)
                           .build();

        Set<UserRole> userRoles = roles.stream().map(role ->
                                                             UserRole.builder()
                                                                     .role(role)
                                                                     .build()
        ).collect(Collectors.toSet());

        UserProfile userProfile = UserProfile.builder()
                                             .displayName(registerRequest.getDisplayName())
                                             .dateOfBirth(registerRequest.getDateOfBirth())
                                             .build();

        userRoles.forEach(newUser::addUserRole);
        newUser.setUserProfile(userProfile);

        userRepository.save(newUser);

        log.info("User {} registered successfully", newUser.getUsername());
        return createAuthenticationResponse(newUser);
    }

    @Override
    public AuthenticationResponse refreshToken(RefreshRequest refreshRequest) {
        log.info("Refresh token attempt");

        if (tokenBlacklistService.isTokenBlacklisted(refreshRequest.getRefreshToken())) {
            log.warn("Refresh token is blacklisted");
            throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        long userId = Long.parseLong(jwtUtil.extractUserID(refreshRequest.getRefreshToken()));
        User user = userRepository.findByIdAndIsActiveTrue(userId)
                                  .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        log.info("Refresh token successful for user ID: {}", userId);
        return createAuthenticationResponse(user);
    }

    @Override
    public Boolean logout(LogoutRequest logoutRequest) {
        log.info("Logout attempt with access token: {}", logoutRequest.getAccessToken());

        long tokenExpiration = jwtUtil.getTokenExpiration(logoutRequest.getAccessToken());
        long refreshTokenExpiration = jwtUtil.getTokenExpiration(logoutRequest.getRefreshToken());

        if (!tokenBlacklistService.blacklistToken(logoutRequest.getRefreshToken(), tokenExpiration)) {
            log.error("Failed to blacklist access token");
            return false;
        }

        if (!tokenBlacklistService.blacklistToken(logoutRequest.getAccessToken(), refreshTokenExpiration)) {
            log.error("Failed to blacklist refresh token");
            return false;
        }

        log.info("Tokens blacklisted successfully for logout");
        return true;
    }

    private AuthenticationResponse createAuthenticationResponse(User user) {
        List<String> roleNames = userRepository.findUserRolesByUserId(user.getId())
                                               .orElse(new ArrayList<>())
                                               .stream().map(userRole -> userRole.getRole().getName())
                                               .toList();
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roleNames);
        claims.put("kyc_status", user.getKycStatus());

        return AuthenticationResponse.builder()
                                     .accessToken(jwtUtil.generateToken(user.getId().toString(), claims))
                                     .refreshToken(jwtUtil.generateRefreshToken(user.getId().toString(), claims))
                                     .userBaseInfo(UserBaseInfo.builder()
                                                               .id(String.valueOf(user.getId()))
                                                               .displayName(user.getUserProfile().getDisplayName())
                                                               .avatarUrl(user.getUserProfile().getAvatarUrl())
                                                               .roles(roleNames)
                                                               .build())
                                     .build();
    }
}
