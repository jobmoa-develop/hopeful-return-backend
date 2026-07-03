package com.jobmoa.hopefulreturn.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
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

    public String createAccessToken(Long userId, String loginId, String role) {
        return buildToken(userId, loginId, role, accessTokenValidityMs);
    }

    public String createRefreshToken(String loginId) {
        return buildToken(null, loginId, null, refreshTokenValidityMs);
    }

    private String buildToken(Long userId, String subject, String role, long validityMs) {
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
        if (role != null) {
            builder.claim("role", role);
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

    public String getRole(String token) {
        Object role = parse(token).get("role");
        return role != null ? role.toString() : null;
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
