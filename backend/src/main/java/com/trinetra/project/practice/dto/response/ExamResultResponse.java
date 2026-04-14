package com.trinetra.project.practice.dto.response;

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
public class ExamResultResponse {

    private String topic;
    private Double score;
    private Integer totalMarks;
    private Integer timeTaken;
    private String type;
    private Instant attemptedAt;
    private Double percentage;
    private List<PracticeAttemptResponse> attempts;
    private PracticePaginationResponse pagination;
}
