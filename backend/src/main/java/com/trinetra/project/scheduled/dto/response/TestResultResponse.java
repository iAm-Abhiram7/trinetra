package com.trinetra.project.scheduled.dto.response;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestResultResponse {

    private String testId;
    private String title;
    private String type;
    private Double score;
    private Integer totalMarks;
    private Double negativeMarking;
    private Integer timeTaken;
    private Double percentage;
    private Instant attemptedAt;
    private TestResultBreakdownResponse breakdown;
}
