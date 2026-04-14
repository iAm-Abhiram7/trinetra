package com.trinetra.project.scheduled.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestResultBreakdownResponse {

    private Integer attempted;
    private Integer correct;
    private Integer wrong;
    private Integer skipped;
}
