package com.trinetra.project.auth.controller;

import com.trinetra.project.auth.dto.request.StudentLoginRequest;
import com.trinetra.project.auth.dto.request.StudentSignupRequest;
import com.trinetra.project.auth.dto.response.StudentAuthUserResponse;
import com.trinetra.project.auth.dto.response.StudentLoginResponse;
import com.trinetra.project.auth.service.AuthService;
import com.trinetra.project.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<StudentAuthUserResponse>> signup(@Valid @RequestBody StudentSignupRequest request) {
        StudentAuthUserResponse response = authService.signupStudent(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Account created successfully. Awaiting admin approval.", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<StudentLoginResponse>> login(@Valid @RequestBody StudentLoginRequest request) {
        StudentLoginResponse response = authService.loginStudent(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful.", response));
    }
}
