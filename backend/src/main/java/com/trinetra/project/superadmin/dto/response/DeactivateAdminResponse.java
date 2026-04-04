package com.trinetra.project.superadmin.dto.response;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeactivateAdminResponse {

    private String adminId;
    private String name;
    private String email;
    private Boolean isActive;
    private Instant deactivatedAt;
}
