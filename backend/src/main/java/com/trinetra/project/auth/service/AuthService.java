package com.trinetra.project.auth.service;

import com.trinetra.project.auth.dto.request.AdminLoginRequest;
import com.trinetra.project.auth.dto.request.StudentLoginRequest;
import com.trinetra.project.auth.dto.request.StudentSignupRequest;
import com.trinetra.project.auth.dto.request.UpdatePasswordRequest;
import com.trinetra.project.auth.dto.request.UpdateProfileRequest;
import com.trinetra.project.auth.dto.request.VerifyOtpRequest;
import com.trinetra.project.auth.dto.response.AdminLoginResponse;
import com.trinetra.project.auth.dto.response.ProfileResponse;
import com.trinetra.project.auth.dto.response.StudentAuthUserResponse;
import com.trinetra.project.auth.dto.response.StudentLoginResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    StudentAuthUserResponse signupStudent(StudentSignupRequest request);

    StudentLoginResponse loginStudent(StudentLoginRequest request);

    StudentAuthUserResponse updateStudentProfile(HttpServletRequest request, UpdateProfileRequest requestBody);

    ProfileResponse getStudentProfile(HttpServletRequest request);

    void initiatePasswordChange(HttpServletRequest request, UpdatePasswordRequest requestBody);

    void completePasswordChange(HttpServletRequest request, VerifyOtpRequest requestBody);

    AdminLoginResponse loginAdmin(AdminLoginRequest request);
}
