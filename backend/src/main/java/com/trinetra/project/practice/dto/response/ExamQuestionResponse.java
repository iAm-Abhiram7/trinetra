package com.trinetra.project.practice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamQuestionResponse {

    private String examId;
    private String title;
    private String type;
    private Integer totalQuestions;
    private Integer totalMarks;
    private Double negativeMarking;
    private Integer durationMinutes;
    private ExamCurrentQuestionResponse currentQuestion;
    private Boolean hasNext;
}
