package com.trinetra.project.common.security.claims;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuperAdminClaims {

    private String userId;
    private String role;
    private Boolean isActive;
    private Boolean noScopeRestriction;
    private String platformAccess;
}
