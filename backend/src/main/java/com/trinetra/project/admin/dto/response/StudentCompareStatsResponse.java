package com.trinetra.project.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentCompareStatsResponse {

    private int totalAttempts;
    private double averageScore;
    private double averageTimeTaken;
    private int practiceAttempts;
    private int scheduledAttempts;
}
