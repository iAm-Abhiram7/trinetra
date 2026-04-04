package com.trinetra.project.common.security.claims;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentClaims {

    private String userId;
    private String role;
    private String college;
    private String branch;
    private String state;
    private int yearOfPassing;
    private String approvalStatus;
}
