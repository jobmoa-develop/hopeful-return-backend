package com.jobmoa.hopefulreturn.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobmoa.hopefulreturn.common.ApiResponse;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * 인증되지 않은(익명) 요청이 보호 리소스에 접근할 때 401 을 반환한다.
 * 지정하지 않으면 Spring Security 기본값(Http403ForbiddenEntryPoint)이 사용되어
 * 미인증 요청도 403 으로 응답되므로, 401(인증 필요) / 403(권한 부족)을 분리하기 위해 등록한다.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(@NonNull HttpServletRequest request,
                         @NonNull HttpServletResponse response,
                         @NonNull AuthenticationException authException) throws IOException {
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(errorCode.getMessage()));
    }
}
