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
public class StudentAuthUserResponse {

    private String id;
    private String name;
    private String email;
    private String role;
    private String state;
    private String college;
    private String branch;
    private Integer yearOfPassing;
    private String approvalStatus;
    private Instant createdAt;
}
