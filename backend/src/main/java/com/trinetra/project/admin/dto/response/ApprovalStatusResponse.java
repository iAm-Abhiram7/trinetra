package com.trinetra.project.admin.dto.response;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalStatusResponse {

    private String status;
    private String approvedBy;
    private Instant approvedAt;
    private String rejectionReason;
}
