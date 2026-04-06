package com.trinetra.project.admin.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedApprovalStudentsResponse {

    private List<ApprovalStudentResponse> students;
    private int currentPage;
    private int totalPages;
    private long totalStudents;
    private int limit;
}
