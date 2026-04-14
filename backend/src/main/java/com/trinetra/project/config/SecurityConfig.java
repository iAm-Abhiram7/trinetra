package com.trinetra.project.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trinetra.project.common.response.ApiResponse;
import com.trinetra.project.common.security.JwtAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, ObjectMapper objectMapper) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // NOTE: JWT does not reflect real-time approvalStatus changes.
            // Token remains valid until expiry even if status changes.
            // For immediate revocation, implement token blacklisting (future enhancement).
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/admin/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/signup").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/admin/tests/**", "/admin/test/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/admin/users").hasRole("SUPER_ADMIN")
                .requestMatchers("/admin/admins").hasRole("SUPER_ADMIN")
                .requestMatchers("/admin/results").hasRole("SUPER_ADMIN")
                .requestMatchers("/admin/add-admin").hasRole("SUPER_ADMIN")
                .requestMatchers("/admincred/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/admin/del-admin").hasRole("SUPER_ADMIN")
                .requestMatchers("/auth/**").hasRole("STUDENT")
                .requestMatchers("/profile").hasRole("STUDENT")
                .requestMatchers("/article/**").hasRole("STUDENT")
                .requestMatchers("/exam/**").hasRole("STUDENT")
                .requestMatchers("/result/**").hasRole("STUDENT")
                .requestMatchers("/test/**").hasRole("STUDENT")
                .requestMatchers("/students/**").hasAnyRole("COLLEGE_ADMIN", "SUPER_ADMIN")
                .requestMatchers("/admin/**").hasAnyRole("COLLEGE_ADMIN", "SUPER_ADMIN")
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint())
                .accessDeniedHandler(accessDeniedHandler())
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> writeErrorResponse(response, 401, "Token missing/expired");
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> writeErrorResponse(response, 403, "Access denied");
    }

    private void writeErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(message)));
    }
}
