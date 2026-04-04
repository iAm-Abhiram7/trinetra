package com.trinetra.project.superadmin.service;

import com.trinetra.project.superadmin.dto.response.TestResultSummaryResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface SuperAdminResultService {

    TestResultSummaryResponse getAggregatedResults(HttpServletRequest request, int page, int limit, String testId);
}
