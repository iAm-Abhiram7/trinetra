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
public class RejectStudentRequest {

    @NotBlank(message = "rejectionReason is required")
    private String rejectionReason;
}
