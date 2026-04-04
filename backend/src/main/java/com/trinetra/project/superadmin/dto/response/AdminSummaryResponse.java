package com.trinetra.project.superadmin.dto.response;

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
public class AdminSummaryResponse {

    private String id;
    private String name;
    private String email;
    private String role;
    private Boolean isActive;
    private List<ScopeDto> scopes;
    private Instant createdAt;
    private Instant lastLoginAt;
}
