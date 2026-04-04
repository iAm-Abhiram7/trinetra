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
public class StudentSummaryResponse {

    private String id;
    private String name;
    private String email;
    private String state;
    private String college;
    private String branch;
    private Integer yearOfPassing;
    private String approvalStatus;
    private Instant createdAt;
}
