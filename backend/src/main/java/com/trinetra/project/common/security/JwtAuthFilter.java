package com.trinetra.project.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trinetra.project.common.response.ApiResponse;
import com.trinetra.project.common.security.claims.AdminClaims;
import com.trinetra.project.common.security.claims.StudentClaims;
import com.trinetra.project.common.security.claims.SuperAdminClaims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    public JwtAuthFilter(JwtUtil jwtUtil, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            if (!jwtUtil.isTokenValid(token)) {
                throw new IllegalStateException("Token is invalid or expired");
            }

            String role = jwtUtil.extractRole(token);
            String userId = jwtUtil.extractUserId(token);

            if ("SUPER_ADMIN".equals(role)) {
                SuperAdminClaims superAdminClaims = jwtUtil.extractSuperAdminClaims(token);
                if (!jwtUtil.isSuperAdmin(token)) {
                    throw new IllegalStateException("Invalid super admin token claims");
                }
                request.setAttribute("superAdminClaims", superAdminClaims);
            } else if ("COLLEGE_ADMIN".equals(role)) {
                AdminClaims adminClaims = jwtUtil.extractAdminClaims(token);
                if (!jwtUtil.isCollegeAdmin(token)) {
                    throw new IllegalStateException("Invalid college admin token claims");
                }
                request.setAttribute("adminClaims", adminClaims);
            } else if ("STUDENT".equals(role)) {
                StudentClaims studentClaims = jwtUtil.extractStudentClaims(token);
                request.setAttribute("studentClaims", studentClaims);
            } else {
                throw new IllegalStateException("Unsupported role in token");
            }

            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                userId,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
            );
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);

            filterChain.doFilter(request, response);
        } catch (RuntimeException ex) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(ex.getMessage())));
        }
    }
}
