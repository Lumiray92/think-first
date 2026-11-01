package com.thinkfirst.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final int expirationHours;

    public JwtService(JwtProperties properties) {
        String secret = properties.getSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("security.jwt.secret 至少需要 32 字节，请设置 TF_JWT_SECRET");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationHours = Math.max(1, properties.getExpirationHours());
    }

    public String issue(Long userId, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("email", email)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(expirationHours * 3600L)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /** 解析失败返回 null，由过滤器决定按未登录处理。 */
    public Long parseUserId(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return Long.valueOf(claims.getSubject());
        } catch (Exception e) {
            return null;
        }
    }
}
