package com.velet.identity.dto.response;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class AuthenticationResponse {
    private String accessToken;
    private String refreshToken;
    private UserBaseInfo userBaseInfo;
}
