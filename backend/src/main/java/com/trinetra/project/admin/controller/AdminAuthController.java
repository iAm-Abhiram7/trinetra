package com.trinetra.project.admin.controller;

import com.trinetra.project.auth.dto.request.AdminLoginRequest;
import com.trinetra.project.auth.dto.response.AdminLoginResponse;
import com.trinetra.project.auth.service.AuthService;
import com.trinetra.project.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminAuthController {

    private final AuthService authService;

    public AdminAuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AdminLoginResponse>> login(@Valid @RequestBody AdminLoginRequest request) {
        AdminLoginResponse response = authService.loginAdmin(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful.", response));
    }
}
