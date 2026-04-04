package com.trinetra.project.superadmin.controller;

import com.trinetra.project.common.response.ApiResponse;
import com.trinetra.project.superadmin.dto.response.TestResultSummaryResponse;
import com.trinetra.project.superadmin.service.SuperAdminResultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class SuperAdminResultController {

    private final SuperAdminResultService superAdminResultService;

    public SuperAdminResultController(SuperAdminResultService superAdminResultService) {
        this.superAdminResultService = superAdminResultService;
    }

    @Operation(summary = "Aggregated results", description = "Returns test-level aggregated metrics", tags = {"Super Admin - Results"})
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/results")
    public ResponseEntity<ApiResponse<TestResultSummaryResponse>> getAggregatedResults(
        HttpServletRequest request,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int limit,
        @RequestParam(required = false) String testId
    ) {
        TestResultSummaryResponse response = superAdminResultService.getAggregatedResults(request, page, limit, testId);
        return ResponseEntity.ok(ApiResponse.success("Aggregated results fetched successfully.", response));
    }
}
