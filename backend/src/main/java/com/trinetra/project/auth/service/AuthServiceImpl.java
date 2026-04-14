package com.trinetra.project.auth.service;

import com.trinetra.project.admin.model.CollegeAdmin;
import com.trinetra.project.admin.model.embedded.Scope;
import com.trinetra.project.admin.repository.CollegeAdminRepository;
import com.trinetra.project.auth.dto.request.AdminLoginRequest;
import com.trinetra.project.auth.dto.request.StudentLoginRequest;
import com.trinetra.project.auth.dto.request.StudentSignupRequest;
import com.trinetra.project.auth.dto.request.UpdatePasswordRequest;
import com.trinetra.project.auth.dto.request.UpdateProfileRequest;
import com.trinetra.project.auth.dto.request.VerifyOtpRequest;
import com.trinetra.project.auth.dto.response.AdminLoginResponse;
import com.trinetra.project.auth.dto.response.ProfileApprovalStatusResponse;
import com.trinetra.project.auth.dto.response.ProfileAptitudeHistoryResponse;
import com.trinetra.project.auth.dto.response.ProfileResponse;
import com.trinetra.project.auth.dto.response.StudentAuthUserResponse;
import com.trinetra.project.auth.dto.response.StudentLoginResponse;
import com.trinetra.project.common.exception.ConflictException;
import com.trinetra.project.common.exception.ForbiddenException;
import com.trinetra.project.common.exception.GoneException;
import com.trinetra.project.common.exception.ResourceNotFoundException;
import com.trinetra.project.common.exception.UnauthorizedException;
import com.trinetra.project.common.exception.UnprocessableException;
import com.trinetra.project.common.security.JwtUtil;
import com.trinetra.project.common.security.claims.StudentClaims;
import com.trinetra.project.student.model.Student;
import com.trinetra.project.student.model.embedded.AptitudeHistory;
import com.trinetra.project.student.model.embedded.ApprovalStatus;
import com.trinetra.project.student.repository.StudentRepository;
import com.trinetra.project.superadmin.dto.response.ScopeDto;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.security.SecureRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

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

        String approvalStatus = student.getApprovalStatus() == null ? "PENDING" : student.getApprovalStatus().getStatus();
        if (!"APPROVED".equals(approvalStatus)) {
            if ("PENDING".equals(approvalStatus)) {
                throw new ForbiddenException("Account is PENDING admin approval");
            }
            if ("REJECTED".equals(approvalStatus)) {
                throw new ForbiddenException("Account has been REJECTED");
            }
            throw new ForbiddenException("Account is not approved");
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
    public StudentAuthUserResponse updateStudentProfile(HttpServletRequest request, UpdateProfileRequest requestBody) {
        StudentClaims claims = verifyApprovedStudent(request);
        Student student = findStudentOrThrow(claims.getUserId());

        String oldCollege = student.getCollege();
        String oldBranch = student.getBranch();

        if (StringUtils.hasText(requestBody.getName())) {
            student.setName(requestBody.getName().trim());
        }
        if (StringUtils.hasText(requestBody.getState())) {
            student.setState(requestBody.getState().trim());
        }
        if (StringUtils.hasText(requestBody.getCollege())) {
            student.setCollege(requestBody.getCollege().trim());
        }
        if (StringUtils.hasText(requestBody.getBranch())) {
            student.setBranch(requestBody.getBranch().trim());
        }
        if (requestBody.getYearOfPassing() != null) {
            student.setYearOfPassing(requestBody.getYearOfPassing());
        }

        boolean collegeChanged = oldCollege != null && student.getCollege() != null && !oldCollege.equalsIgnoreCase(student.getCollege());
        boolean branchChanged = oldBranch != null && student.getBranch() != null && !oldBranch.equalsIgnoreCase(student.getBranch());

        if (collegeChanged || branchChanged) {
            student.setApprovalStatus(
                ApprovalStatus.builder()
                    .status("PENDING")
                    .approvedBy(null)
                    .approvedAt(null)
                    .rejectionReason(null)
                    .build()
            );
            LOGGER.warn("Student {} college/branch changed, approval reset to PENDING", student.getId());
        }

        Student savedStudent = studentRepository.save(student);
        return toStudentAuthUserResponse(savedStudent);
    }

    @Override
    public ProfileResponse getStudentProfile(HttpServletRequest request) {
        StudentClaims claims = verifyApprovedStudent(request);
        Student student = findStudentOrThrow(claims.getUserId());

        return ProfileResponse.builder()
            .id(student.getId())
            .name(student.getName())
            .email(student.getEmail())
            .role(student.getRole())
            .state(student.getState())
            .college(student.getCollege())
            .branch(student.getBranch())
            .yearOfPassing(student.getYearOfPassing())
            .approvalStatus(
                student.getApprovalStatus() == null
                    ? null
                    : ProfileApprovalStatusResponse.builder()
                        .status(student.getApprovalStatus().getStatus())
                        .approvedBy(student.getApprovalStatus().getApprovedBy())
                        .approvedAt(student.getApprovalStatus().getApprovedAt())
                        .rejectionReason(student.getApprovalStatus().getRejectionReason())
                        .build()
            )
            .aptitudeHistory(
                student.getAptitudeHistory() == null
                    ? new ArrayList<>()
                    : student.getAptitudeHistory().stream().map(this::toProfileAptitudeHistoryResponse).toList()
            )
            .createdAt(student.getCreatedAt())
            .build();
    }

    @Override
    public void initiatePasswordChange(HttpServletRequest request, UpdatePasswordRequest requestBody) {
        StudentClaims claims = verifyApprovedStudent(request);
        Student student = findStudentOrThrow(claims.getUserId());

        if (!passwordEncoder.matches(requestBody.getCurrentPassword(), student.getPasswordHash())) {
            throw new UnauthorizedException("Current password incorrect");
        }

        int otpValue = 100000 + SECURE_RANDOM.nextInt(900000);
        String otp = String.valueOf(otpValue);

        student.setOtpHash(passwordEncoder.encode(otp));
        student.setOtpExpiry(Instant.now().plusSeconds(300));
        studentRepository.save(student);

        // TODO: Replace with actual email service (JavaMail/SendGrid).
        LOGGER.info("OTP for student {}: {} (expires in 5 min)", student.getEmail(), otp);
    }

    @Override
    public void completePasswordChange(HttpServletRequest request, VerifyOtpRequest requestBody) {
        StudentClaims claims = verifyApprovedStudent(request);
        Student student = findStudentOrThrow(claims.getUserId());

        if (student.getOtpExpiry() == null || student.getOtpHash() == null) {
            throw new UnprocessableException("Invalid OTP");
        }

        if (Instant.now().isAfter(student.getOtpExpiry())) {
            throw new GoneException("OTP expired");
        }

        if (!passwordEncoder.matches(requestBody.getOtp(), student.getOtpHash())) {
            throw new UnprocessableException("Invalid OTP");
        }

        student.setPasswordHash(passwordEncoder.encode(requestBody.getNewPassword()));
        student.setOtpHash(null);
        student.setOtpExpiry(null);
        studentRepository.save(student);
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

    private ProfileAptitudeHistoryResponse toProfileAptitudeHistoryResponse(AptitudeHistory history) {
        return ProfileAptitudeHistoryResponse.builder()
            .topic(history.getTopic())
            .score(history.getScore())
            .timeTaken(history.getTimeTaken())
            .type(history.getType())
            .examId(history.getExamId())
            .attemptedAt(history.getAttemptedAt())
            .attempted(history.getAttempted())
            .correct(history.getCorrect())
            .wrong(history.getWrong())
            .skipped(history.getSkipped())
            .build();
    }

    private StudentClaims verifyApprovedStudent(HttpServletRequest request) {
        StudentClaims claims = (StudentClaims) request.getAttribute("studentClaims");
        if (claims == null) {
            throw new UnauthorizedException("Token missing");
        }

        if (!"APPROVED".equals(claims.getApprovalStatus())) {
            throw new ForbiddenException("Account not approved");
        }

        return claims;
    }

    private Student findStudentOrThrow(String studentId) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("No account with that email"));

        if (!"STUDENT".equals(student.getRole()) || Boolean.TRUE.equals(student.getIsDeleted())) {
            throw new ResourceNotFoundException("No account with that email");
        }
        return student;
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
