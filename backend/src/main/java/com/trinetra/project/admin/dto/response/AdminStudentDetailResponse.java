package com.trinetra.project.admin.dto.response;

import com.trinetra.project.student.model.embedded.AptitudeHistory;
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
public class AdminStudentDetailResponse {

    private String id;
    private String name;
    private String email;
    private String role;
    private String state;
    private String college;
    private String branch;
    private Integer yearOfPassing;
    private ApprovalStatusResponse approvalStatus;
    private List<AptitudeHistory> aptitudeHistory;
    private Instant createdAt;
}
