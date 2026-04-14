package com.trinetra.project.auth.dto.response;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileApprovalStatusResponse {

    private String status;
    private String approvedBy;
    private Instant approvedAt;
    private String rejectionReason;
}
