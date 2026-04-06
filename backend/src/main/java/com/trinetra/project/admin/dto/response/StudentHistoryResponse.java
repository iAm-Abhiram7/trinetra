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
public class StudentHistoryResponse {

    private String studentId;
    private String studentName;
    private String college;
    private String branch;
    private List<HistoryActionResponse> actions;
}
