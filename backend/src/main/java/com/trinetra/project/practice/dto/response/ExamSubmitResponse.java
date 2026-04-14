package com.trinetra.project.practice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamSubmitResponse {

    private String examId;
    private String topic;
    private String type;
    private Integer totalQuestions;
    private Integer attempted;
    private Integer correct;
    private Integer wrong;
    private Integer skipped;
    private Double score;
    private Integer totalMarks;
    private Integer timeTaken;
    private Double percentage;
}
