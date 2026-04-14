package com.trinetra.project.scheduled.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestSubmitResponse {

    private String testId;
    private String title;
    private String type;
    private Integer totalQuestions;
    private Integer attempted;
    private Integer correct;
    private Integer wrong;
    private Integer skipped;
    private Double score;
    private Integer totalMarks;
    private Double negativeMarking;
    private Integer timeTaken;
    private Double percentage;
}
