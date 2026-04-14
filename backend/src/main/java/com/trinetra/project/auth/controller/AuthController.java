package com.trinetra.project.auth.controller;

import com.trinetra.project.auth.dto.request.StudentLoginRequest;
import com.trinetra.project.auth.dto.request.StudentSignupRequest;
import com.trinetra.project.auth.dto.request.UpdatePasswordRequest;
import com.trinetra.project.auth.dto.request.UpdateProfileRequest;
import com.trinetra.project.auth.dto.request.VerifyOtpRequest;
import com.trinetra.project.auth.dto.response.ProfileResponse;
import com.trinetra.project.auth.dto.response.StudentAuthUserResponse;
import com.trinetra.project.auth.dto.response.StudentLoginResponse;
import com.trinetra.project.auth.service.AuthService;
import com.trinetra.project.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Auth - Student Authentication")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Student signup", description = "Registers a student account with PENDING approval")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Student registered successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "Email already exists",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
        }
    )
    @PostMapping("/auth/signup")
    public ResponseEntity<ApiResponse<StudentAuthUserResponse>> signup(@Valid @RequestBody StudentSignupRequest request) {
        StudentAuthUserResponse response = authService.signupStudent(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Account created successfully. Awaiting admin approval.", response));
    }

    @Operation(summary = "Student login", description = "Authenticates approved students and issues JWT")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Student not approved",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
        }
    )
    @PostMapping("/auth/login")
    public ResponseEntity<ApiResponse<StudentLoginResponse>> login(@Valid @RequestBody StudentLoginRequest request) {
        StudentLoginResponse response = authService.loginStudent(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful.", response));
    }

    @Operation(summary = "Update student profile", description = "Updates student profile and resets approval when college or branch changes")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Unauthorized",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
        }
    )
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/auth/update-profile")
    public ResponseEntity<ApiResponse<StudentAuthUserResponse>> updateProfile(
        HttpServletRequest request,
        @Valid @RequestBody UpdateProfileRequest requestBody
    ) {
        StudentAuthUserResponse response = authService.updateStudentProfile(request, requestBody);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully.", response));
    }

    @Operation(summary = "Get student profile", description = "Returns profile details and full aptitude history for logged in student")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile fetched successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Unauthorized",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
        }
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(HttpServletRequest request) {
        ProfileResponse response = authService.getStudentProfile(request);
        return ResponseEntity.ok(ApiResponse.success("Profile fetched successfully.", response));
    }

    @Operation(summary = "Initiate password update", description = "Validates current password and sends OTP to registered email")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OTP generated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Unauthorized",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
        }
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/auth/update-password")
    public ResponseEntity<ApiResponse<Map<String, String>>> initiatePasswordChange(
        HttpServletRequest request,
        @Valid @RequestBody UpdatePasswordRequest requestBody
    ) {
        authService.initiatePasswordChange(request, requestBody);
        return ResponseEntity.ok(
            ApiResponse.success(
                "OTP sent to your registered email. Valid for 5 minutes.",
                Map.of("message", "OTP sent to your registered email. Valid for 5 minutes.")
            )
        );
    }

    @Operation(summary = "Complete password update", description = "Verifies OTP and updates student password")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Password updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "410",
                description = "OTP expired",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
        }
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/auth/update-pass/otp")
    public ResponseEntity<ApiResponse<Map<String, String>>> completePasswordChange(
        HttpServletRequest request,
        @Valid @RequestBody VerifyOtpRequest requestBody
    ) {
        authService.completePasswordChange(request, requestBody);
        return ResponseEntity.ok(ApiResponse.success("Password updated successfully.", Map.of("message", "Password updated successfully.")));
    }
}
