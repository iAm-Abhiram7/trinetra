package com.trinetra.project.auth.dto.response;

import com.trinetra.project.superadmin.dto.response.ScopeDto;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminLoginResponse {

    private String token;
    private String id;
    private String name;
    private String email;
    private String role;
    private Boolean isActive;
    private List<ScopeDto> scopes;
    private Instant lastLoginAt;
}
