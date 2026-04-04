package com.trinetra.project.superadmin.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestResultSummaryResponse {

    private List<AggregatedResultResponse> testResults;
    private int currentPage;
    private int totalPages;
    private long totalTests;
    private int limit;
}
