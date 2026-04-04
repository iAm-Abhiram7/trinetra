package com.trinetra.project.superadmin.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamDetailResponse {

    private String id;
    private String title;
    private String type;
    private Boolean isPublished;
    private Integer totalQuestions;
    private String createdBy;
    private List<QuestionClientResponse> questions;
}
