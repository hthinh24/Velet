package com.velet.identity.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "role_permissions")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class RolePermission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id")
    private Permission permission;
}

