package com.trinetra.project.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trinetra.project.admin.model.CollegeAdmin;
import com.trinetra.project.admin.model.embedded.Scope;
import com.trinetra.project.common.exception.UnauthorizedException;
import com.trinetra.project.common.security.claims.AdminClaims;
import com.trinetra.project.common.security.claims.StudentClaims;
import com.trinetra.project.common.security.claims.SuperAdminClaims;
import com.trinetra.project.student.model.Student;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long studentExpiryMs;
    private final long adminExpiryMs;
    private final long superAdminExpiryMs;
    private final ObjectMapper objectMapper;

    public JwtUtil(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.expiry.student}") long studentExpiryMs,
        @Value("${jwt.expiry.admin}") long adminExpiryMs,
        @Value("${jwt.expiry.superadmin}") long superAdminExpiryMs,
        ObjectMapper objectMapper
    ) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("jwt.secret must be at least 32 characters");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.studentExpiryMs = studentExpiryMs;
        this.adminExpiryMs = adminExpiryMs;
        this.superAdminExpiryMs = superAdminExpiryMs;
        this.objectMapper = objectMapper;
    }

    public String generateStudentToken(Student student) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "STUDENT");
        claims.put("college", student.getCollege());
        claims.put("branch", student.getBranch());
        claims.put("state", student.getState());
        claims.put("yearOfPassing", student.getYearOfPassing());
        claims.put("approvalStatus", student.getApprovalStatus() == null ? null : student.getApprovalStatus().getStatus());
        return buildToken(student.getId(), studentExpiryMs, claims);
    }

    public String generateAdminToken(CollegeAdmin admin) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "COLLEGE_ADMIN");
        claims.put("isActive", admin.getIsActive());
        claims.put("scopes", admin.getScopes() == null ? new ArrayList<>() : admin.getScopes());
        return buildToken(admin.getId(), adminExpiryMs, claims);
    }

    public String generateSuperAdminToken(CollegeAdmin superAdmin) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "SUPER_ADMIN");
        claims.put("isActive", superAdmin.getIsActive());
        claims.put("noScopeRestriction", true);
        claims.put("platformAccess", "FULL");
        return buildToken(superAdmin.getId(), superAdminExpiryMs, claims);
    }

    public StudentClaims extractStudentClaims(String token) {
        Claims claims = parseClaims(token);
        String role = claims.get("role", String.class);
        if (!"STUDENT".equals(role)) {
            throw new UnauthorizedException("Invalid student token role");
        }

        String userId = claims.getSubject();
        String college = claims.get("college", String.class);
        String branch = claims.get("branch", String.class);
        String state = claims.get("state", String.class);
        Integer year = toInteger(claims.get("yearOfPassing"));
        String approvalStatus = claims.get("approvalStatus", String.class);

        if (userId == null || college == null || branch == null || state == null || year == null || approvalStatus == null) {
            throw new UnauthorizedException("Student token is missing required claims");
        }

        return StudentClaims.builder()
            .userId(userId)
            .role(role)
            .college(college)
            .branch(branch)
            .state(state)
            .yearOfPassing(year)
            .approvalStatus(approvalStatus)
            .build();
    }

    public AdminClaims extractAdminClaims(String token) {
        Claims claims = parseClaims(token);
        String role = claims.get("role", String.class);
        if (!"COLLEGE_ADMIN".equals(role)) {
            throw new UnauthorizedException("Invalid admin token role");
        }

        String userId = claims.getSubject();
        Boolean isActive = claims.get("isActive", Boolean.class);
        List<Scope> scopes = extractScopes(claims.get("scopes"));

        if (userId == null || isActive == null) {
            throw new UnauthorizedException("Admin token is missing required claims");
        }

        return AdminClaims.builder()
            .userId(userId)
            .role(role)
            .isActive(isActive)
            .scopes(scopes)
            .build();
    }

    public SuperAdminClaims extractSuperAdminClaims(String token) {
        Claims claims = parseClaims(token);
        String role = claims.get("role", String.class);
        if (!"SUPER_ADMIN".equals(role)) {
            throw new UnauthorizedException("Invalid super admin token role");
        }

        String userId = claims.getSubject();
        Boolean isActive = claims.get("isActive", Boolean.class);
        Boolean noScopeRestriction = claims.get("noScopeRestriction", Boolean.class);
        String platformAccess = claims.get("platformAccess", String.class);

        if (userId == null || isActive == null || noScopeRestriction == null || platformAccess == null) {
            throw new UnauthorizedException("Super admin token is missing required claims");
        }

        return SuperAdminClaims.builder()
            .userId(userId)
            .role(role)
            .isActive(isActive)
            .noScopeRestriction(noScopeRestriction)
            .platformAccess(platformAccess)
            .build();
    }

    public String extractRole(String token) {
        Claims claims = parseClaims(token);
        String role = claims.get("role", String.class);
        if (role == null) {
            throw new UnauthorizedException("Token role claim is missing");
        }
        return role;
    }

    public String extractUserId(String token) {
        String subject = parseClaims(token).getSubject();
        if (subject == null || subject.isBlank()) {
            throw new UnauthorizedException("Token subject claim is missing");
        }
        return subject;
    }

    public boolean isTokenValid(String token) {
        Claims claims = parseClaims(token);
        Date expiration = claims.getExpiration();
        return expiration != null && expiration.toInstant().isAfter(Instant.now());
    }

    public boolean isSuperAdmin(String token) {
        SuperAdminClaims claims = extractSuperAdminClaims(token);
        return "SUPER_ADMIN".equals(claims.getRole())
            && Boolean.TRUE.equals(claims.getNoScopeRestriction())
            && "FULL".equals(claims.getPlatformAccess())
            && Boolean.TRUE.equals(claims.getIsActive());
    }

    public boolean isCollegeAdmin(String token) {
        AdminClaims claims = extractAdminClaims(token);
        return "COLLEGE_ADMIN".equals(claims.getRole()) && Boolean.TRUE.equals(claims.getIsActive());
    }

    private String buildToken(String subject, long expiryMs, Map<String, Object> claims) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expiryMs);

        return Jwts.builder()
            .subject(subject)
            .claims(claims)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(secretKey)
            .compact();
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
        } catch (Exception ex) {
            throw new UnauthorizedException("Invalid or expired token");
        }
    }

    private Integer toInteger(Object value) {
        if (value instanceof Integer integer) {
            return integer;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private List<Scope> extractScopes(Object rawScopes) {
        if (!(rawScopes instanceof List<?> rawList)) {
            return new ArrayList<>();
        }

        List<Scope> scopes = new ArrayList<>();
        for (Object entry : rawList) {
            Scope scope = objectMapper.convertValue(entry, Scope.class);
            scopes.add(scope);
        }
        return scopes;
    }
}
