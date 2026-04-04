package com.trinetra.project.superadmin.service;

import com.trinetra.project.superadmin.dto.request.AddAdminRequest;
import com.trinetra.project.superadmin.dto.request.DeactivateAdminRequest;
import com.trinetra.project.superadmin.dto.response.AdminCredResponse;
import com.trinetra.project.superadmin.dto.response.AdminSummaryResponse;
import com.trinetra.project.superadmin.dto.response.DeactivateAdminResponse;
import com.trinetra.project.superadmin.dto.response.PagedAdminsResponse;
import com.trinetra.project.superadmin.dto.response.PagedStudentsResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface SuperAdminUserService {

    PagedStudentsResponse getAllStudents(
        HttpServletRequest request,
        int page,
        int limit,
        String state,
        String college,
        String branch,
        String status,
        String search
    );

    PagedAdminsResponse getAllAdmins(
        HttpServletRequest request,
        int page,
        int limit,
        String state,
        String college,
        Boolean isActive
    );

    AdminSummaryResponse createCollegeAdmin(HttpServletRequest request, AddAdminRequest requestBody);

    AdminCredResponse getAdminCredentials(HttpServletRequest request, String college, String branch);

    DeactivateAdminResponse deactivateAdmin(HttpServletRequest request, DeactivateAdminRequest requestBody);
}
