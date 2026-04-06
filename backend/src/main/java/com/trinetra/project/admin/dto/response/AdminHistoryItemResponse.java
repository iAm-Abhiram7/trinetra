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
public class AdminHistoryItemResponse {

    private String studentId;
    private String studentName;
    private String email;
    private String college;
    private String branch;
    private String action;
    private Instant actionAt;
    private String rejectionReason;
}
