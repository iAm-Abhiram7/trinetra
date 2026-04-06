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
public class CompareStudentsRequest {

    @NotBlank(message = "studentId1 is required")
    private String studentId1;

    @NotBlank(message = "studentId2 is required")
    private String studentId2;
}
