package com.trinetra.project.admin.dto.response;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestStatusRowResponse {

    private String studentId;
    private String name;
    private String branch;
    private Double score;
    private Integer timeTaken;
    private Double percentage;
    private Instant attemptedAt;
}
