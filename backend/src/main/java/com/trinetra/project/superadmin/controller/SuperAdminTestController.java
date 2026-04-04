package com.trinetra.project.superadmin.controller;

import com.trinetra.project.common.response.ApiResponse;
import com.trinetra.project.superadmin.dto.request.CreateExamRequest;
import com.trinetra.project.superadmin.dto.response.DeleteTestResponse;
import com.trinetra.project.superadmin.dto.response.ExamDetailResponse;
import com.trinetra.project.superadmin.dto.response.PagedTestsResponse;
import com.trinetra.project.superadmin.service.SuperAdminTestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class SuperAdminTestController {

    private final SuperAdminTestService superAdminTestService;

    public SuperAdminTestController(SuperAdminTestService superAdminTestService) {
        this.superAdminTestService = superAdminTestService;
    }

    @Operation(summary = "List tests", description = "Returns super admin tests with filters", tags = {"Super Admin - Tests"})
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/tests")
    public ResponseEntity<ApiResponse<PagedTestsResponse>> getAllTests(
        HttpServletRequest request,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int limit,
        @RequestParam(required = false) String type,
        @RequestParam(required = false) Boolean isPublished,
        @RequestParam(required = false) String search
    ) {
        PagedTestsResponse response = superAdminTestService.getAllTests(request, page, limit, type, isPublished, search);
        return ResponseEntity.ok(ApiResponse.success("Tests fetched successfully.", response));
    }

    @Operation(summary = "Create test", description = "Creates a scheduled or practice exam", tags = {"Super Admin - Tests"})
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/test/create")
    public ResponseEntity<ApiResponse<ExamDetailResponse>> createTest(
        HttpServletRequest request,
        @Valid @RequestBody CreateExamRequest requestBody
    ) {
        ExamDetailResponse response = superAdminTestService.createTest(request, requestBody);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Test created successfully.", response));
    }

    @Operation(summary = "Delete test", description = "Soft deletes a test created by this super admin", tags = {"Super Admin - Tests"})
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/test/{testId}")
    public ResponseEntity<ApiResponse<DeleteTestResponse>> deleteTest(
        HttpServletRequest request,
        @PathVariable String testId
    ) {
        DeleteTestResponse response = superAdminTestService.deleteTest(request, testId);
        return ResponseEntity.ok(ApiResponse.success("Test deleted successfully.", response));
    }
}
