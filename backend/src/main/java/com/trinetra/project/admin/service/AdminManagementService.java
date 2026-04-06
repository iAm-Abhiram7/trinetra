package com.trinetra.project.admin.service;

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
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;

public interface AdminManagementService {

    /**
     * Returns pending students visible to the authenticated admin.
     */
    PagedApprovalStudentsResponse getPendingStudents(
        HttpServletRequest request,
        int page,
        int limit,
        String branch,
        String search
    );

    /**
     * Returns rejected students acted on by the authenticated admin.
     */
    PagedApprovalStudentsResponse getRejectedStudents(
        HttpServletRequest request,
        int page,
        int limit,
        String branch,
        String search
    );

    /**
     * Approves a single pending student within admin scope.
     */
    StudentApprovalActionResponse approveStudent(HttpServletRequest request, String studentId);

    /**
     * Approves all pending students within admin scope.
     */
    BulkApproveResponse approveAllStudents(HttpServletRequest request, BulkApproveRequest requestBody);

    /**
     * Rejects a single pending student within admin scope.
     */
    StudentApprovalActionResponse rejectStudent(HttpServletRequest request, String studentId, RejectStudentRequest requestBody);

    /**
     * Rejects all pending students within admin scope using a rejection reason.
     */
    BulkRejectResponse rejectAllStudents(HttpServletRequest request, BulkRejectRequest requestBody);

    /**
     * Returns full student details for a scoped student.
     */
    AdminStudentDetailResponse getStudentDetail(HttpServletRequest request, String studentId);

    /**
     * Compares two scoped students and computed statistics.
     */
    StudentCompareResponse compareStudents(HttpServletRequest request, CompareStudentsRequest requestBody);

    /**
     * Returns paginated attempted student results for the requested test.
     */
    PagedTestStatusResponse getTestStatus(
        HttpServletRequest request,
        String testId,
        int page,
        int limit,
        String sortBy,
        String order
    );

    /**
     * Returns admin action history (latest-only per student with current schema).
     */
    PagedAdminHistoryResponse getHistory(
        HttpServletRequest request,
        int page,
        int limit,
        String action,
        String branch,
        Instant from,
        Instant to
    );

    /**
     * Returns history for a single student acted on by the authenticated admin.
     */
    StudentHistoryResponse getStudentHistory(HttpServletRequest request, String studentId);
}
