package com.trinetra.project.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentApprovalActionResponse {

    private String id;
    private String name;
    private ApprovalStatusResponse approvalStatus;
}
