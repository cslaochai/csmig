package com.mig.auth.util;

import com.mig.auth.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类（与 cslk HIS 的 JwtUtils 同方案：jjwt，claims 带 userId/username/currentRole）。
 */
@Component
@RequiredArgsConstructor
public class JwtUtils {

    private final JwtProperties jwtProperties;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 JWT Token。
     */
    public String generateToken(Long userId, String username, String currentRole) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId == null ? null : String.valueOf(userId));
        claims.put("username", username);
        claims.put("currentRole", currentRole);

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getExpiration()))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 从 Token 中获取当前角色。
     */
    public String getCurrentRoleFromToken(String token) {
        return getClaimsFromToken(token).get("currentRole", String.class);
    }

    /**
     * 从 Token 中获取用户名。
     */
    public String getUsernameFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    /**
     * 从 Token 中获取用户 ID。
     */
    public Long getUserIdFromToken(String token) {
        return toLong(getClaimsFromToken(token).get("userId"));
    }

    /**
     * 验证 Token 是否有效。
     */
    public boolean validateToken(String token) {
        try {
            getClaimsFromToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
