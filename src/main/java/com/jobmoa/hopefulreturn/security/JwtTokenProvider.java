package com.jobmoa.hopefulreturn.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessTokenValidityMs;
    private final long refreshTokenValidityMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity}") long accessTokenValidityMs,
            @Value("${jwt.refresh-token-validity}") long refreshTokenValidityMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidityMs = accessTokenValidityMs;
        this.refreshTokenValidityMs = refreshTokenValidityMs;
    }

    public String createAccessToken(Long userId, String loginId, List<String> roles) {
        return createAccessToken(userId, loginId, roles, false);
    }

    public String createAccessToken(Long userId, String loginId, List<String> roles, boolean canSendSms) {
        return buildToken(userId, loginId, roles, canSendSms, accessTokenValidityMs);
    }

    public String createRefreshToken(String loginId) {
        return buildToken(null, loginId, null, false, refreshTokenValidityMs);
    }

    private String buildToken(Long userId, String subject, List<String> roles, boolean canSendSms, long validityMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validityMs);
        var builder = Jwts.builder()
                .subject(subject)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key);
        if (userId != null) {
            builder.claim("userId", userId);
        }
        if (roles != null && !roles.isEmpty()) {
            builder.claim("roles", roles);
        }
        if (canSendSms) {
            builder.claim("canSendSms", true);
        }
        return builder.compact();
    }

    public Long getUserId(String token) {
        Object userId = parse(token).get("userId");
        if (userId instanceof Number number) {
            return number.longValue();
        }
        return userId != null ? Long.valueOf(userId.toString()) : null;
    }

    public String getLoginId(String token) {
        return parse(token).getSubject();
    }

    public String getEmail(String token) {
        return getLoginId(token);
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        Object roles = parse(token).get("roles");
        if (roles instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return Collections.emptyList();
    }

    public boolean getCanSendSms(String token) {
        Object value = parse(token).get("canSendSms");
        return value instanceof Boolean flag ? flag : Boolean.parseBoolean(String.valueOf(value));
    }

    public boolean validate(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public long getAccessTokenValiditySeconds() {
        return accessTokenValidityMs / 1000;
    }

    public long getRefreshTokenValiditySeconds() {
        return refreshTokenValidityMs / 1000;
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}