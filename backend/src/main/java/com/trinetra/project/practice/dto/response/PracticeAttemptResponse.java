package com.trinetra.project.practice.dto.response;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PracticeAttemptResponse {

    private Double score;
    private Integer timeTaken;
    private Instant attemptedAt;
}
