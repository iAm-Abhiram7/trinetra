package com.trinetra.project.superadmin.controller;

import com.trinetra.project.common.response.ApiResponse;
import com.trinetra.project.superadmin.dto.request.AddAdminRequest;
import com.trinetra.project.superadmin.dto.request.DeactivateAdminRequest;
import com.trinetra.project.superadmin.dto.response.AdminCredResponse;
import com.trinetra.project.superadmin.dto.response.AdminSummaryResponse;
import com.trinetra.project.superadmin.dto.response.DeactivateAdminResponse;
import com.trinetra.project.superadmin.dto.response.PagedAdminsResponse;
import com.trinetra.project.superadmin.dto.response.PagedStudentsResponse;
import com.trinetra.project.superadmin.service.SuperAdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class SuperAdminUserController {

    private final SuperAdminUserService superAdminUserService;

    public SuperAdminUserController(SuperAdminUserService superAdminUserService) {
        this.superAdminUserService = superAdminUserService;
    }

    @Operation(summary = "List students", description = "Returns platform students for super admin", tags = {"Super Admin - Users"})
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/admin/users")
    public ResponseEntity<ApiResponse<PagedStudentsResponse>> getAllStudents(
        HttpServletRequest request,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "50") int limit,
        @RequestParam(required = false) String state,
        @RequestParam(required = false) String college,
        @RequestParam(required = false) String branch,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String search
    ) {
        PagedStudentsResponse response = superAdminUserService.getAllStudents(
            request,
            page,
            limit,
            state,
            college,
            branch,
            status,
            search
        );
        return ResponseEntity.ok(ApiResponse.success("Students fetched successfully.", response));
    }

    @Operation(summary = "List admins", description = "Returns college admins for super admin", tags = {"Super Admin - Admins"})
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/admin/admins")
    public ResponseEntity<ApiResponse<PagedAdminsResponse>> getAllAdmins(
        HttpServletRequest request,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int limit,
        @RequestParam(required = false) String state,
        @RequestParam(required = false) String college,
        @RequestParam(required = false) Boolean isActive
    ) {
        PagedAdminsResponse response = superAdminUserService.getAllAdmins(request, page, limit, state, college, isActive);
        return ResponseEntity.ok(ApiResponse.success("Admins fetched successfully.", response));
    }

    @Operation(summary = "Add admin", description = "Creates a college admin account", tags = {"Super Admin - Admins"})
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/admin/add-admin")
    public ResponseEntity<ApiResponse<AdminSummaryResponse>> addAdmin(
        HttpServletRequest request,
        @Valid @RequestBody AddAdminRequest requestBody
    ) {
        AdminSummaryResponse response = superAdminUserService.createCollegeAdmin(request, requestBody);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Admin account created successfully.", response));
    }

    @Operation(summary = "Admin credentials lookup", description = "Fetches admin account details by college and branch", tags = {"Super Admin - Admins"})
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping({"/admincred", "/admincred/"})
    public ResponseEntity<ApiResponse<AdminCredResponse>> getAdminCredentials(
        HttpServletRequest request,
        @RequestParam String college,
        @RequestParam String branch
    ) {
        AdminCredResponse response = superAdminUserService.getAdminCredentials(request, college, branch);
        return ResponseEntity.ok(ApiResponse.success("Admin credentials fetched successfully.", response));
    }

    @Operation(summary = "Deactivate admin", description = "Deactivates a college admin account", tags = {"Super Admin - Admins"})
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/admin/del-admin")
    public ResponseEntity<ApiResponse<DeactivateAdminResponse>> deactivateAdmin(
        HttpServletRequest request,
        @Valid @RequestBody DeactivateAdminRequest requestBody
    ) {
        DeactivateAdminResponse response = superAdminUserService.deactivateAdmin(request, requestBody);
        return ResponseEntity.ok(ApiResponse.success("Admin account deactivated successfully.", response));
    }
}
