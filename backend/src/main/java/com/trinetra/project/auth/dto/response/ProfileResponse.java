package com.trinetra.project.auth.dto.response;

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
public class ProfileResponse {

    private String id;
    private String name;
    private String email;
    private String role;
    private String state;
    private String college;
    private String branch;
    private Integer yearOfPassing;
    private ProfileApprovalStatusResponse approvalStatus;
    private List<ProfileAptitudeHistoryResponse> aptitudeHistory;
    private Instant createdAt;
}
