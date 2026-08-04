package com.jobmoa.hopefulreturn.auth.service;

import com.jobmoa.hopefulreturn.auth.model.dto.LoginRequest;
import com.jobmoa.hopefulreturn.auth.model.dto.LoginResponse;
import com.jobmoa.hopefulreturn.auth.model.dto.MeResponse;
import com.jobmoa.hopefulreturn.auth.model.dto.RefreshResponse;
import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.security.JwtTokenProvider;
import com.jobmoa.hopefulreturn.userrole.entity.UserRoleEntity;
import com.jobmoa.hopefulreturn.users.entity.UsersEntity;
import com.jobmoa.hopefulreturn.users.repository.UsersRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.jobmoa.hopefulreturn.auth.model.dto.ChangePasswordRequest;
import com.jobmoa.hopefulreturn.auth.model.dto.UpdateMyProfileRequest;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private static final String TOKEN_TYPE = "Bearer";
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final String COOKIE_PATH = "/";
    private static final String SAME_SITE = "Lax";

    // ADMIN·HEAD_OFFICE 는 계정 플래그(can_send_sms)와 무관하게 항상 문자 발송 권한을 갖는다.
    // 이 두 역할이 다른 계정에게 can_send_sms 플래그를 수동으로 부여/회수할 수 있다.
    private static final Set<String> AUTO_SMS_ROLES = Set.of("ADMIN", "HEAD_OFFICE");

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    // RefreshToken 쿠키 Secure 속성(app.cookie.secure). 기본 true(HTTPS).
    // HTTP 평문 배포에서는 COOKIE_SECURE=false 로 주입해야 브라우저가 쿠키를 저장·전송한다.
    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    @Override
    public LoginResponse login(LoginRequest request, HttpServletResponse response) {
        UsersEntity user = usersRepository.findByLoginIdAndDeletedFalse(request.loginId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        List<String> roles = extractRoleNames(user);
        boolean canSendSms = resolveCanSendSms(user, roles);
        String accessToken = jwtTokenProvider.createAccessToken(user.getUserId(), user.getLoginId(), roles, canSendSms);
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getLoginId());

        user.setRefreshToken(refreshToken);
        usersRepository.save(user);
        addRefreshTokenCookie(response, refreshToken);

        LoginResponse.User responseUser = new LoginResponse.User(
                user.getUserId(),
                user.getLoginId(),
                user.getName(),
                user.getPhone(),
                user.getEmail(),
                roles,
                canSendSms);
        return new LoginResponse(accessToken, TOKEN_TYPE, jwtTokenProvider.getAccessTokenValiditySeconds(), responseUser);
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshResponse refresh(HttpServletRequest request) {
        String refreshToken = resolveRefreshToken(request);
        if (!jwtTokenProvider.validate(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        String loginId = jwtTokenProvider.getLoginId(refreshToken);
        UsersEntity user = usersRepository.findByLoginIdAndDeletedFalse(loginId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));
        if (!Objects.equals(refreshToken, user.getRefreshToken())) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        List<String> roles = extractRoleNames(user);
        String accessToken = jwtTokenProvider.createAccessToken(
                user.getUserId(), user.getLoginId(), roles, resolveCanSendSms(user, roles));
        return new RefreshResponse(accessToken, TOKEN_TYPE, jwtTokenProvider.getAccessTokenValiditySeconds());
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = findRefreshTokenCookie(request);
        if (refreshToken != null && jwtTokenProvider.validate(refreshToken)) {
            String loginId = jwtTokenProvider.getLoginId(refreshToken);
            usersRepository.findByLoginIdAndDeletedFalse(loginId)
                    .filter(user -> Objects.equals(refreshToken, user.getRefreshToken()))
                    .ifPresent(user -> {
                        user.setRefreshToken(null);
                        usersRepository.save(user);
                    });
        }
        deleteRefreshTokenCookie(response);
    }

    @Override
    @Transactional(readOnly = true)
    public MeResponse me() {
        String loginId = getCurrentLoginId();
        UsersEntity user = usersRepository.findByLoginIdAndDeletedFalse(loginId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        List<String> roles = extractRoleNames(user);
        return new MeResponse(
                user.getUserId(),
                user.getLoginId(),
                user.getName(),
                user.getPhone(),
                user.getEmail(),
                roles,
                resolveCanSendSms(user, roles));
    }

    @Override
    public MeResponse updateMyProfile(UpdateMyProfileRequest request) {
        if (request.phone() == null && request.email() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        String loginId = getCurrentLoginId();
        UsersEntity user = usersRepository.findByLoginIdAndDeletedFalse(loginId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (request.phone() != null) {
            user.setPhone(request.phone());
        }
        if (request.email() != null) {
            user.setEmail(request.email());
        }
        user.setUpdatedAt(java.time.LocalDateTime.now());
        usersRepository.save(user);

        List<String> roles = extractRoleNames(user);
        return new MeResponse(
                user.getUserId(),
                user.getLoginId(),
                user.getName(),
                user.getPhone(),
                user.getEmail(),
                roles,
                resolveCanSendSms(user, roles));
    }

    @Override
    public void changePassword(ChangePasswordRequest request) {
        String loginId = getCurrentLoginId();
        UsersEntity user = usersRepository.findByLoginIdAndDeletedFalse(loginId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        // 비밀번호 변경 시 다른 기기의 세션도 함께 끊기도록 refreshToken 무효화
        user.setRefreshToken(null);
        user.setUpdatedAt(java.time.LocalDateTime.now());
        usersRepository.save(user);
    }

    /**
     * 문자 발송 권한 판정: DB 플래그(can_send_sms) 또는 ADMIN·HEAD_OFFICE 역할 보유 시 true.
     * ADMIN·HEAD_OFFICE 는 플래그 값과 무관하게 항상 권한을 가지며,
     * 그 외 계정은 ADMIN·HEAD_OFFICE 가 수동으로 부여한 플래그로만 권한을 가진다.
     */
    private boolean resolveCanSendSms(UsersEntity user, List<String> roles) {
        return Boolean.TRUE.equals(user.getCanSendSms())
                || roles.stream().anyMatch(AUTO_SMS_ROLES::contains);
    }

    private String getCurrentLoginId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        return authentication.getName();
    }

    private List<String> extractRoleNames(UsersEntity user) {
        if (user.getUserRoles() == null || user.getUserRoles().isEmpty()) {
            return List.of();
        }
        return user.getUserRoles().stream()
                .map(UserRoleEntity::getRole)
                .filter(role -> role != null && role.getRoleName() != null)
                .map(role -> role.getRoleName().name())
                .toList();
    }

    private String resolveRefreshToken(HttpServletRequest request) {
        String refreshToken = findRefreshTokenCookie(request);
        if (refreshToken == null) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        return refreshToken;
    }

    private String findRefreshTokenCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(SAME_SITE)
                .path(COOKIE_PATH)
                .maxAge(jwtTokenProvider.getRefreshTokenValiditySeconds())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void deleteRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(SAME_SITE)
                .path(COOKIE_PATH)
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}