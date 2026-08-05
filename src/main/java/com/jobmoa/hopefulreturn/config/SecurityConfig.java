package com.jobmoa.hopefulreturn.config;

import com.jobmoa.hopefulreturn.security.JwtAccessDeniedHandler;
import com.jobmoa.hopefulreturn.security.JwtAuthenticationEntryPoint;
import com.jobmoa.hopefulreturn.security.JwtAuthenticationFilter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // 공개 엔드포인트. 인증/회원 도메인이 확정되면 여기에 인증 관련 경로를 추가한다.
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/ping",
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/logout",

            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/v3/api-docs",

            "/actuator/health",
            "/actuator/health/**"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                // 미인증 → 401(EntryPoint), 인증됐으나 권한부족 → 403(AccessDeniedHandler)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        // 그 외 API 는 인증 필요
                        .requestMatchers("/api/**").authenticated()
                        // 정적 파일 + SPA 셸(index.html)은 공개. 실제 데이터는 /api/** 뒤에서 JWT 로 보호된다.
                        .anyRequest().permitAll())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    // 추후 비밀번호 해싱이 필요할 때 재사용 (현재 미사용이나 인프라로 유지)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 시스템 관리자(ADMIN)를 슈퍼유저로 취급하기 위한 역할 계층.
     * ROLE_ADMIN 이 나머지 모든 역할(RoleName enum 기준 8개)을 함축하므로,
     * hasRole/hasAnyRole 로 보호되는 현재·미래의 모든 엔드포인트를 ADMIN 이 통과한다.
     *
     * 주의: RoleHierarchy 는 인가 표현식 평가 시점에만 적용되고 Authentication.getAuthorities()
     * (원본 JWT 권한)는 바꾸지 않는다. 따라서 AuthScopeSupport 의 데이터 스코프 판정
     * (isCounselorOnly / isStaffOnly / hasUnrestrictedScope)에는 영향이 없다.
     * static 으로 선언해 method security 초기화 순서 이슈를 피한다.
     */
    @Bean
    static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role("ADMIN").implies(
                        "HEAD_OFFICE", "REGIONAL_MANAGER", "OPERATOR", "COUNSELOR",
                        "STAFF", "LECTURER", "PROJECT_MANAGER", "PROJECT_LEADER")
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("http://localhost:*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
