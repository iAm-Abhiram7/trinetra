package com.trinetra.project.auth.dto.response;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileAptitudeHistoryResponse {

    private String topic;
    private Double score;
    private Integer timeTaken;
    private String type;
    private String examId;
    private Instant attemptedAt;
    private Integer attempted;
    private Integer correct;
    private Integer wrong;
    private Integer skipped;
}
