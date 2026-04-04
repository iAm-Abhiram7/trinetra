package com.trinetra.project.superadmin.service;

import com.trinetra.project.admin.model.CollegeAdmin;
import com.trinetra.project.admin.model.embedded.Scope;
import com.trinetra.project.admin.repository.CollegeAdminRepository;
import com.trinetra.project.common.exception.ConflictException;
import com.trinetra.project.common.exception.ForbiddenException;
import com.trinetra.project.common.exception.ResourceNotFoundException;
import com.trinetra.project.common.exception.ValidationException;
import com.trinetra.project.common.security.claims.SuperAdminClaims;
import com.trinetra.project.student.model.Student;
import com.trinetra.project.superadmin.dto.request.AddAdminRequest;
import com.trinetra.project.superadmin.dto.request.DeactivateAdminRequest;
import com.trinetra.project.superadmin.dto.response.AdminCredResponse;
import com.trinetra.project.superadmin.dto.response.AdminSummaryResponse;
import com.trinetra.project.superadmin.dto.response.DeactivateAdminResponse;
import com.trinetra.project.superadmin.dto.response.PagedAdminsResponse;
import com.trinetra.project.superadmin.dto.response.PagedStudentsResponse;
import com.trinetra.project.superadmin.dto.response.ScopeDto;
import com.trinetra.project.superadmin.dto.response.StudentSummaryResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SuperAdminUserServiceImpl implements SuperAdminUserService {

    private final MongoTemplate usersMongoTemplate;
    private final CollegeAdminRepository collegeAdminRepository;
    private final PasswordEncoder passwordEncoder;

    public SuperAdminUserServiceImpl(
        @Qualifier("usersMongoTemplate") MongoTemplate usersMongoTemplate,
        CollegeAdminRepository collegeAdminRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.usersMongoTemplate = usersMongoTemplate;
        this.collegeAdminRepository = collegeAdminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public PagedStudentsResponse getAllStudents(
        HttpServletRequest request,
        int page,
        int limit,
        String state,
        String college,
        String branch,
        String status,
        String search
    ) {
        verifySuperAdmin(request);

        int safePage = page < 1 ? 1 : page;
        int safeLimit = limit < 1 ? 50 : limit;

        Query query = new Query();
        query.addCriteria(Criteria.where("role").is("STUDENT"));
        query.addCriteria(notDeletedCriteria());

        if (StringUtils.hasText(state)) {
            query.addCriteria(Criteria.where("state").is(state));
        }
        if (StringUtils.hasText(college)) {
            query.addCriteria(Criteria.where("college").is(college));
        }
        if (StringUtils.hasText(branch)) {
            query.addCriteria(Criteria.where("branch").is(branch));
        }
        if (StringUtils.hasText(status)) {
            query.addCriteria(Criteria.where("approvalStatus.status").is(status.toUpperCase()));
        }
        if (StringUtils.hasText(search)) {
            query.addCriteria(
                new Criteria().orOperator(
                    Criteria.where("name").regex(search, "i"),
                    Criteria.where("email").regex(search, "i")
                )
            );
        }

        Query countQuery = Query.of(query).limit(-1).skip(-1);

        query
            .with(Sort.by(Sort.Direction.DESC, "createdAt"))
            .skip((long) (safePage - 1) * safeLimit)
            .limit(safeLimit);

        List<Student> students = usersMongoTemplate.find(query, Student.class);
        long totalStudents = usersMongoTemplate.count(countQuery, Student.class);

        int totalPages = totalStudents == 0 ? 0 : (int) Math.ceil((double) totalStudents / safeLimit);

        return PagedStudentsResponse.builder()
            .students(students.stream().map(this::toStudentSummary).toList())
            .currentPage(safePage)
            .totalPages(totalPages)
            .totalStudents(totalStudents)
            .limit(safeLimit)
            .build();
    }

    @Override
    public PagedAdminsResponse getAllAdmins(
        HttpServletRequest request,
        int page,
        int limit,
        String state,
        String college,
        Boolean isActive
    ) {
        verifySuperAdmin(request);

        int safePage = page < 1 ? 1 : page;
        int safeLimit = limit < 1 ? 20 : limit;

        Query query = new Query();
        query.addCriteria(Criteria.where("role").is("COLLEGE_ADMIN"));
        query.addCriteria(notDeletedCriteria());

        if (isActive != null) {
            query.addCriteria(Criteria.where("isActive").is(isActive));
        }

        if (StringUtils.hasText(state) || StringUtils.hasText(college)) {
            Criteria elem = new Criteria();
            List<Criteria> scopeCriteria = new ArrayList<>();
            if (StringUtils.hasText(state)) {
                scopeCriteria.add(Criteria.where("state").is(state));
            }
            if (StringUtils.hasText(college)) {
                scopeCriteria.add(Criteria.where("college").is(college));
            }
            elem.andOperator(scopeCriteria.toArray(new Criteria[0]));
            query.addCriteria(Criteria.where("scopes").elemMatch(elem));
        }

        Query countQuery = Query.of(query).limit(-1).skip(-1);
        query
            .with(Sort.by(Sort.Direction.DESC, "createdAt"))
            .skip((long) (safePage - 1) * safeLimit)
            .limit(safeLimit);

        List<CollegeAdmin> admins = usersMongoTemplate.find(query, CollegeAdmin.class);
        long totalAdmins = usersMongoTemplate.count(countQuery, CollegeAdmin.class);

        int totalPages = totalAdmins == 0 ? 0 : (int) Math.ceil((double) totalAdmins / safeLimit);

        return PagedAdminsResponse.builder()
            .admins(admins.stream().map(this::toAdminSummary).toList())
            .currentPage(safePage)
            .totalPages(totalPages)
            .totalAdmins(totalAdmins)
            .limit(safeLimit)
            .build();
    }

    @Override
    public AdminSummaryResponse createCollegeAdmin(HttpServletRequest request, AddAdminRequest requestBody) {
        verifySuperAdmin(request);

        collegeAdminRepository.findByEmailIgnoreCase(requestBody.getEmail()).ifPresent(existing -> {
            if (!Boolean.TRUE.equals(existing.getIsDeleted())) {
                throw new ConflictException("Email already registered");
            }
        });

        List<Scope> scopes = new ArrayList<>();
        for (ScopeDto scopeDto : requestBody.getScopes()) {
            scopes.add(
                Scope.builder()
                    .state(scopeDto.getState())
                    .college(scopeDto.getCollege())
                    .branch(scopeDto.getBranch())
                    .build()
            );
        }

        CollegeAdmin admin = CollegeAdmin.builder()
            .name(requestBody.getName())
            .email(requestBody.getEmail().toLowerCase())
            .passwordHash(passwordEncoder.encode(requestBody.getPassword()))
            .role("COLLEGE_ADMIN")
            .isActive(true)
            .scopes(scopes)
            .lastLoginAt(null)
            .isDeleted(false)
            .deletedAt(null)
            .deletedBy(null)
            .createdAt(Instant.now())
            .build();

        CollegeAdmin savedAdmin = collegeAdminRepository.save(admin);
        return toAdminSummary(savedAdmin);
    }

    @Override
    public AdminCredResponse getAdminCredentials(HttpServletRequest request, String college, String branch) {
        verifySuperAdmin(request);

        if (!StringUtils.hasText(college) || !StringUtils.hasText(branch)) {
            throw new ValidationException("college and branch are required");
        }

        Query query = new Query();
        query.addCriteria(Criteria.where("role").is("COLLEGE_ADMIN"));
        query.addCriteria(notDeletedCriteria());
        query.addCriteria(
            Criteria.where("scopes").elemMatch(
                Criteria.where("college").is(college).and("branch").is(branch)
            )
        );

        CollegeAdmin admin = usersMongoTemplate.findOne(query, CollegeAdmin.class);
        if (admin == null) {
            throw new ResourceNotFoundException("No admin for this college + branch");
        }

        return AdminCredResponse.builder()
            .adminId(admin.getId())
            .name(admin.getName())
            .email(admin.getEmail())
            .college(college)
            .branch(branch)
            .isActive(admin.getIsActive())
            .build();
    }

    @Override
    public DeactivateAdminResponse deactivateAdmin(HttpServletRequest request, DeactivateAdminRequest requestBody) {
        SuperAdminClaims claims = verifySuperAdmin(request);

        Query query = new Query();
        query.addCriteria(Criteria.where("role").is("COLLEGE_ADMIN"));
        query.addCriteria(notDeletedCriteria());
        query.addCriteria(
            Criteria.where("scopes").elemMatch(
                Criteria.where("college").is(requestBody.getCollege()).and("branch").is(requestBody.getBranch())
            )
        );

        CollegeAdmin admin = usersMongoTemplate.findOne(query, CollegeAdmin.class);
        if (admin == null) {
            throw new ResourceNotFoundException("No admin for this college + branch");
        }

        if (!Boolean.TRUE.equals(admin.getIsActive()) || Boolean.TRUE.equals(admin.getIsDeleted())) {
            throw new ConflictException("Admin already deactivated");
        }

        Instant now = Instant.now();
        admin.setIsActive(false);
        admin.setIsDeleted(true);
        admin.setDeletedAt(now);
        admin.setDeletedBy(claims.getUserId());
        collegeAdminRepository.save(admin);

        return DeactivateAdminResponse.builder()
            .adminId(admin.getId())
            .name(admin.getName())
            .email(admin.getEmail())
            .isActive(false)
            .deactivatedAt(now)
            .build();
    }

    private SuperAdminClaims verifySuperAdmin(HttpServletRequest request) {
        SuperAdminClaims claims = (SuperAdminClaims) request.getAttribute("superAdminClaims");
        if (
            claims == null
                || !Boolean.TRUE.equals(claims.getNoScopeRestriction())
                || !"FULL".equals(claims.getPlatformAccess())
        ) {
            throw new ForbiddenException("Super Admin access required");
        }
        return claims;
    }

    private Criteria notDeletedCriteria() {
        return new Criteria().orOperator(
            Criteria.where("isDeleted").exists(false),
            Criteria.where("isDeleted").is(false)
        );
    }

    private StudentSummaryResponse toStudentSummary(Student student) {
        return StudentSummaryResponse.builder()
            .id(student.getId())
            .name(student.getName())
            .email(student.getEmail())
            .state(student.getState())
            .college(student.getCollege())
            .branch(student.getBranch())
            .yearOfPassing(student.getYearOfPassing())
            .approvalStatus(student.getApprovalStatus() == null ? null : student.getApprovalStatus().getStatus())
            .createdAt(student.getCreatedAt())
            .build();
    }

    private AdminSummaryResponse toAdminSummary(CollegeAdmin admin) {
        return AdminSummaryResponse.builder()
            .id(admin.getId())
            .name(admin.getName())
            .email(admin.getEmail())
            .role(admin.getRole())
            .isActive(admin.getIsActive())
            .scopes(toScopeDtos(admin.getScopes()))
            .createdAt(admin.getCreatedAt())
            .lastLoginAt(admin.getLastLoginAt())
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
