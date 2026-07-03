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
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private static final String TOKEN_TYPE = "Bearer";
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final String COOKIE_PATH = "/";
    private static final String SAME_SITE = "Lax";

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public LoginResponse login(LoginRequest request, HttpServletResponse response) {
        UsersEntity user = usersRepository.findByLoginIdAndDeletedFalse(request.loginId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        String role = extractRoleName(user);
        String accessToken = jwtTokenProvider.createAccessToken(user.getUserId(), user.getLoginId(), role);
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
                role);
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

        String role = extractRoleName(user);
        String accessToken = jwtTokenProvider.createAccessToken(user.getUserId(), user.getLoginId(), role);
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
        return new MeResponse(
                user.getUserId(),
                user.getLoginId(),
                user.getName(),
                user.getPhone(),
                user.getEmail(),
                extractRoleName(user));
    }

    private String getCurrentLoginId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        return authentication.getName();
    }

    private String extractRoleName(UsersEntity user) {
        if (user.getUserRoles() == null || user.getUserRoles().isEmpty()) {
            return null;
        }
        return user.getUserRoles().stream()
                .map(UserRoleEntity::getRole)
                .filter(role -> role != null && role.getRoleName() != null)
                .map(role -> role.getRoleName().name())
                .findFirst()
                .orElse(null);
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
                .secure(true)
                .sameSite(SAME_SITE)
                .path(COOKIE_PATH)
                .maxAge(jwtTokenProvider.getRefreshTokenValiditySeconds())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void deleteRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(true)
                .sameSite(SAME_SITE)
                .path(COOKIE_PATH)
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
