package com.trinetra.project.auth.service;

import com.trinetra.project.auth.dto.request.AdminLoginRequest;
import com.trinetra.project.auth.dto.request.StudentLoginRequest;
import com.trinetra.project.auth.dto.request.StudentSignupRequest;
import com.trinetra.project.auth.dto.response.AdminLoginResponse;
import com.trinetra.project.auth.dto.response.StudentAuthUserResponse;
import com.trinetra.project.auth.dto.response.StudentLoginResponse;

public interface AuthService {

    StudentAuthUserResponse signupStudent(StudentSignupRequest request);

    StudentLoginResponse loginStudent(StudentLoginRequest request);

    AdminLoginResponse loginAdmin(AdminLoginRequest request);
}
