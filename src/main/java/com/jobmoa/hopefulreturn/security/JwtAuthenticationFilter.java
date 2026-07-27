package com.jobmoa.hopefulreturn.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null
                && jwtTokenProvider.validate(token)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            Long userId = jwtTokenProvider.getUserId(token);
            String loginId = jwtTokenProvider.getLoginId(token);
            List<String> roles = jwtTokenProvider.getRoles(token);
            List<SimpleGrantedAuthority> authorities = new ArrayList<>(roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .toList());
            // 계정 단위 문자 발송 권한 → SMS_SEND authority (역할과 별개, ROLE 접두사 없음).
            // ADMIN·HEAD_OFFICE 는 canSendSms 클레임 값과 무관하게 role만으로도 항상 부여한다
            // (구버전 토큰 등 canSendSms 클레임이 누락된 엣지케이스 방어).
            boolean canSendSms = jwtTokenProvider.getCanSendSms(token)
                    || roles.contains("ADMIN")
                    || roles.contains("HEAD_OFFICE");
            if (canSendSms) {
                authorities.add(new SimpleGrantedAuthority("SMS_SEND"));
            }
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(loginId, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            request.setAttribute("userId", userId);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader(AUTH_HEADER);
        if (StringUtils.hasText(bearer) && bearer.startsWith(BEARER_PREFIX)) {
            return bearer.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}