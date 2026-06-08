package com.velet.identity.dto.response;

import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class UserBaseInfo {
    private String id;
    private String displayName;
    private String avatarUrl;
    private List<String> roles;
}
