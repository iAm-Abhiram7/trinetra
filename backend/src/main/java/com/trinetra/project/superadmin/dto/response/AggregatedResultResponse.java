package com.trinetra.project.superadmin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AggregatedResultResponse {

    private String testId;
    private String title;
    private String type;
    private String college;
    private String branch;
    private long totalStudentsAttempted;
    private double averageScore;
    private double highestScore;
    private double lowestScore;
    private double averageTimeTaken;
    private double averagePercentage;
}
