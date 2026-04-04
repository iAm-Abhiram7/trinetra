package com.trinetra.project.auth.service;

import com.trinetra.project.admin.model.CollegeAdmin;
import com.trinetra.project.admin.model.embedded.Scope;
import com.trinetra.project.admin.repository.CollegeAdminRepository;
import com.trinetra.project.auth.dto.request.AdminLoginRequest;
import com.trinetra.project.auth.dto.request.StudentLoginRequest;
import com.trinetra.project.auth.dto.request.StudentSignupRequest;
import com.trinetra.project.auth.dto.response.AdminLoginResponse;
import com.trinetra.project.auth.dto.response.StudentAuthUserResponse;
import com.trinetra.project.auth.dto.response.StudentLoginResponse;
import com.trinetra.project.common.exception.ConflictException;
import com.trinetra.project.common.exception.ForbiddenException;
import com.trinetra.project.common.exception.ResourceNotFoundException;
import com.trinetra.project.common.exception.UnauthorizedException;
import com.trinetra.project.common.security.JwtUtil;
import com.trinetra.project.student.model.Student;
import com.trinetra.project.student.model.embedded.ApprovalStatus;
import com.trinetra.project.student.repository.StudentRepository;
import com.trinetra.project.superadmin.dto.response.ScopeDto;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final StudentRepository studentRepository;
    private final CollegeAdminRepository collegeAdminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final String superAdminUsername;
    private final String superAdminPassword;

    public AuthServiceImpl(
        StudentRepository studentRepository,
        CollegeAdminRepository collegeAdminRepository,
        PasswordEncoder passwordEncoder,
        JwtUtil jwtUtil,
        @Value("${security.superadmin.username}") String superAdminUsername,
        @Value("${security.superadmin.password}") String superAdminPassword
    ) {
        this.studentRepository = studentRepository;
        this.collegeAdminRepository = collegeAdminRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.superAdminUsername = superAdminUsername;
        this.superAdminPassword = superAdminPassword;
    }

    @Override
    public StudentAuthUserResponse signupStudent(StudentSignupRequest request) {
        studentRepository.findByEmailIgnoreCase(request.getEmail()).ifPresent(existing -> {
            if (!Boolean.TRUE.equals(existing.getIsDeleted())) {
                throw new ConflictException("Email already registered");
            }
        });

        Student student = Student.builder()
            .name(request.getName())
            .email(request.getEmail().toLowerCase())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .role("STUDENT")
            .state(request.getState())
            .college(request.getCollege())
            .branch(request.getBranch())
            .yearOfPassing(request.getYearOfPassing())
            .approvalStatus(
                ApprovalStatus.builder()
                    .status("PENDING")
                    .approvedBy(null)
                    .approvedAt(null)
                    .rejectionReason(null)
                    .build()
            )
            .aptitudeHistory(new ArrayList<>())
            .isDeleted(false)
            .deletedAt(null)
            .deletedBy(null)
            .createdAt(Instant.now())
            .lastLoginAt(null)
            .build();

        Student savedStudent = studentRepository.save(student);
        return toStudentAuthUserResponse(savedStudent);
    }

    @Override
    public StudentLoginResponse loginStudent(StudentLoginRequest request) {
        Student student = studentRepository.findByEmailIgnoreCase(request.getEmail())
            .orElseThrow(() -> new ResourceNotFoundException("No account with that email"));

        if (Boolean.TRUE.equals(student.getIsDeleted())) {
            throw new ResourceNotFoundException("No account with that email");
        }

        if (!passwordEncoder.matches(request.getPassword(), student.getPasswordHash())) {
            throw new UnauthorizedException("Wrong password");
        }

        if (student.getApprovalStatus() == null || !"APPROVED".equals(student.getApprovalStatus().getStatus())) {
            throw new ForbiddenException("Account is PENDING or REJECTED");
        }

        student.setLastLoginAt(Instant.now());
        studentRepository.save(student);

        String token = jwtUtil.generateStudentToken(student);
        return StudentLoginResponse.builder()
            .token(token)
            .user(toStudentAuthUserResponse(student))
            .build();
    }

    @Override
    public AdminLoginResponse loginAdmin(AdminLoginRequest request) {
        if (superAdminUsername.equals(request.getEmail()) && superAdminPassword.equals(request.getPassword())) {
            CollegeAdmin superAdmin = CollegeAdmin.builder()
                .id("SUPER_ADMIN_ROOT")
                .name("Platform Super Admin")
                .email(superAdminUsername)
                .role("SUPER_ADMIN")
                .isActive(true)
                .scopes(new ArrayList<>())
                .lastLoginAt(Instant.now())
                .build();

            return AdminLoginResponse.builder()
                .token(jwtUtil.generateSuperAdminToken(superAdmin))
                .id(superAdmin.getId())
                .name(superAdmin.getName())
                .email(superAdmin.getEmail())
                .role(superAdmin.getRole())
                .isActive(true)
                .scopes(new ArrayList<>())
                .lastLoginAt(superAdmin.getLastLoginAt())
                .build();
        }

        CollegeAdmin admin = collegeAdminRepository.findByEmailIgnoreCase(request.getEmail())
            .orElseThrow(() -> new ResourceNotFoundException("No admin with this email"));

        if (Boolean.TRUE.equals(admin.getIsDeleted())) {
            throw new ResourceNotFoundException("No admin with this email");
        }

        if (!passwordEncoder.matches(request.getPassword(), admin.getPasswordHash())) {
            throw new UnauthorizedException("Wrong password");
        }

        if (!Boolean.TRUE.equals(admin.getIsActive())) {
            throw new ForbiddenException("Admin deactivated");
        }

        admin.setLastLoginAt(Instant.now());
        collegeAdminRepository.save(admin);

        return AdminLoginResponse.builder()
            .token(jwtUtil.generateAdminToken(admin))
            .id(admin.getId())
            .name(admin.getName())
            .email(admin.getEmail())
            .role(admin.getRole())
            .isActive(admin.getIsActive())
            .scopes(toScopeDtos(admin.getScopes()))
            .lastLoginAt(admin.getLastLoginAt())
            .build();
    }

    private StudentAuthUserResponse toStudentAuthUserResponse(Student student) {
        return StudentAuthUserResponse.builder()
            .id(student.getId())
            .name(student.getName())
            .email(student.getEmail())
            .role(student.getRole())
            .state(student.getState())
            .college(student.getCollege())
            .branch(student.getBranch())
            .yearOfPassing(student.getYearOfPassing())
            .approvalStatus(student.getApprovalStatus() == null ? null : student.getApprovalStatus().getStatus())
            .createdAt(student.getCreatedAt())
            .build();
    }

    private List<ScopeDto> toScopeDtos(List<Scope> scopes) {
        List<ScopeDto> dtos = new ArrayList<>();
        if (scopes == null) {
            return dtos;
        }

        for (Scope scope : scopes) {
            dtos.add(
                ScopeDto.builder()
                    .state(scope.getState())
                    .college(scope.getCollege())
                    .branch(scope.getBranch())
                    .build()
            );
        }
        return dtos;
    }
}
