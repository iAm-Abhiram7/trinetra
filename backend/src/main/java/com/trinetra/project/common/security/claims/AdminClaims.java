package com.trinetra.project.common.security.claims;

import com.trinetra.project.admin.model.embedded.Scope;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminClaims {

    private String userId;
    private String role;
    private Boolean isActive;
    private List<Scope> scopes;
}
