package com.trinetra.project.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkRejectRequest {

    @NotBlank(message = "rejectionReason is required")
    private String rejectionReason;

    private String branch;
}
