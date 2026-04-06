package com.trinetra.project.admin.controller;

import com.trinetra.project.admin.dto.request.BulkApproveRequest;
import com.trinetra.project.admin.dto.request.BulkRejectRequest;
import com.trinetra.project.admin.dto.request.CompareStudentsRequest;
import com.trinetra.project.admin.dto.request.RejectStudentRequest;
import com.trinetra.project.admin.dto.response.AdminStudentDetailResponse;
import com.trinetra.project.admin.dto.response.BulkApproveResponse;
import com.trinetra.project.admin.dto.response.BulkRejectResponse;
import com.trinetra.project.admin.dto.response.PagedAdminHistoryResponse;
import com.trinetra.project.admin.dto.response.PagedApprovalStudentsResponse;
import com.trinetra.project.admin.dto.response.PagedTestStatusResponse;
import com.trinetra.project.admin.dto.response.StudentApprovalActionResponse;
import com.trinetra.project.admin.dto.response.StudentCompareResponse;
import com.trinetra.project.admin.dto.response.StudentHistoryResponse;
import com.trinetra.project.admin.service.AdminManagementService;
import com.trinetra.project.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminManagementController {

    private final AdminManagementService adminManagementService;

    public AdminManagementController(AdminManagementService adminManagementService) {
        this.adminManagementService = adminManagementService;
    }

    @Operation(summary = "List pending approvals", description = "Returns pending students within admin scope", tags = {"Admin Approvals"})
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/approvals")
    public ResponseEntity<ApiResponse<PagedApprovalStudentsResponse>> getApprovals(
        HttpServletRequest request,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int limit,
        @RequestParam(required = false) String branch,
        @RequestParam(required = false) String search
    ) {
        PagedApprovalStudentsResponse response = adminManagementService.getPendingStudents(request, page, limit, branch, search);
        return ResponseEntity.ok(ApiResponse.success("Pending approvals fetched successfully.", response));
    }

    @Operation(summary = "List rejected students", description = "Returns rejected students acted on by this admin", tags = {"Admin Approvals"})
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/rejections")
    public ResponseEntity<ApiResponse<PagedApprovalStudentsResponse>> getRejections(
        HttpServletRequest request,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int limit,
        @RequestParam(required = false) String branch,
        @RequestParam(required = false) String search
    ) {
        PagedApprovalStudentsResponse response = adminManagementService.getRejectedStudents(request, page, limit, branch, search);
        return ResponseEntity.ok(ApiResponse.success("Rejected students fetched successfully.", response));
    }

    @Operation(summary = "Approve student", description = "Approves a pending student in scope", tags = {"Admin Approvals"})
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/approve/{id}")
    public ResponseEntity<ApiResponse<StudentApprovalActionResponse>> approveStudent(
        HttpServletRequest request,
        @PathVariable String id
    ) {
        StudentApprovalActionResponse response = adminManagementService.approveStudent(request, id);
        return ResponseEntity.ok(ApiResponse.success("Student approved successfully.", response));
    }

    @Operation(summary = "Approve all pending students", description = "Bulk approves pending students within scope", tags = {"Admin Approvals"})
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/approve/all")
    public ResponseEntity<ApiResponse<BulkApproveResponse>> approveAllStudents(
        HttpServletRequest request,
        @RequestBody(required = false) BulkApproveRequest requestBody
    ) {
        BulkApproveResponse response = adminManagementService.approveAllStudents(request, requestBody);
        return ResponseEntity.ok(ApiResponse.success("All pending students approved.", response));
    }

    @Operation(summary = "Reject student", description = "Rejects a pending student in scope", tags = {"Admin Approvals"})
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/reject/{id}")
    public ResponseEntity<ApiResponse<StudentApprovalActionResponse>> rejectStudent(
        HttpServletRequest request,
        @PathVariable String id,
        @Valid @RequestBody RejectStudentRequest requestBody
    ) {
        StudentApprovalActionResponse response = adminManagementService.rejectStudent(request, id, requestBody);
        return ResponseEntity.ok(ApiResponse.success("Student rejected.", response));
    }

    @Operation(summary = "Reject all pending students", description = "Bulk rejects pending students within scope", tags = {"Admin Approvals"})
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/reject/all")
    public ResponseEntity<ApiResponse<BulkRejectResponse>> rejectAllStudents(
        HttpServletRequest request,
        @Valid @RequestBody BulkRejectRequest requestBody
    ) {
        BulkRejectResponse response = adminManagementService.rejectAllStudents(request, requestBody);
        return ResponseEntity.ok(ApiResponse.success("All pending students rejected.", response));
    }

    @Operation(summary = "Get student details", description = "Returns full student profile for scoped student", tags = {"Admin Students"})
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/student/{id}")
    public ResponseEntity<ApiResponse<AdminStudentDetailResponse>> getStudent(
        HttpServletRequest request,
        @PathVariable String id
    ) {
        AdminStudentDetailResponse response = adminManagementService.getStudentDetail(request, id);
        return ResponseEntity.ok(ApiResponse.success("Student fetched successfully.", response));
    }

    @Operation(summary = "Compare two students", description = "Compares two students and computed performance stats", tags = {"Admin Students"})
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/compare")
    public ResponseEntity<ApiResponse<StudentCompareResponse>> compareStudents(
        HttpServletRequest request,
        @Valid @RequestBody CompareStudentsRequest requestBody
    ) {
        StudentCompareResponse response = adminManagementService.compareStudents(request, requestBody);
        return ResponseEntity.ok(ApiResponse.success("Students compared successfully.", response));
    }

    @Operation(summary = "Test status for students", description = "Returns paginated student attempts for a test", tags = {"Admin Students"})
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/testStatus/{testId}")
    public ResponseEntity<ApiResponse<PagedTestStatusResponse>> getTestStatus(
        HttpServletRequest request,
        @PathVariable String testId,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int limit,
        @RequestParam(defaultValue = "score") String sortBy,
        @RequestParam(defaultValue = "desc") String order
    ) {
        PagedTestStatusResponse response = adminManagementService.getTestStatus(request, testId, page, limit, sortBy, order);
        return ResponseEntity.ok(ApiResponse.success("Test status fetched successfully.", response));
    }

    @Operation(summary = "Get admin history", description = "Returns approval or rejection actions by this admin", tags = {"Admin History"})
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<PagedAdminHistoryResponse>> getHistory(
        HttpServletRequest request,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int limit,
        @RequestParam(required = false) String action,
        @RequestParam(required = false) String branch,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        PagedAdminHistoryResponse response = adminManagementService.getHistory(request, page, limit, action, branch, from, to);
        return ResponseEntity.ok(ApiResponse.success("Admin history fetched successfully.", response));
    }

    @Operation(summary = "Get student history", description = "Returns action history for a specific student by this admin", tags = {"Admin History"})
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/history/{id}")
    public ResponseEntity<ApiResponse<StudentHistoryResponse>> getStudentHistory(
        HttpServletRequest request,
        @PathVariable String id
    ) {
        StudentHistoryResponse response = adminManagementService.getStudentHistory(request, id);
        return ResponseEntity.ok(ApiResponse.success("Student history fetched successfully.", response));
    }
}
