package com.axuanhogan.membership.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
public class JwtTokenProvider implements TokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationInMs;

    @Value("${jwt.pre-auth-expiration-ms}")
    private long jwtPreAuthExpirationInMs;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成登入第一階段的 Pre-Auth Token (效期短，僅用於 2FA 驗證)
     */
    @Override
    public String generatePreAuthToken(String email) {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime expiryDate = now.plus(jwtPreAuthExpirationInMs, ChronoUnit.MILLIS);

        return Jwts.builder()
                .subject(email)
                .claim("token_type", "PRE_AUTH")
                .issuedAt(Date.from(now.toInstant()))
                .expiration(Date.from(expiryDate.toInstant()))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 生成登入第二階段驗證通過後的 Access Token (正式訪問憑證)
     */
    @Override
    public String generateAccessToken(String email) {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime expiryDate = now.plus(jwtExpirationInMs, ChronoUnit.MILLIS);

        return Jwts.builder()
                .subject(email)
                .claim("token_type", "ACCESS")
                .issuedAt(Date.from(now.toInstant()))
                .expiration(Date.from(expiryDate.toInstant()))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 從 Token 提取 Email
     */
    @Override
    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    /**
     * 從 Token 提取 Token 類型 (PRE_AUTH 或 ACCESS)
     */
    @Override
    public String getTokenTypeFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("token_type", String.class);
    }

    /**
     * 驗證 Token 是否合法與過期
     */
    @Override
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
            return true;
        } catch (Exception ex) {
            log.error("JWT 驗證失敗: {}", ex.getMessage());
        }
        return false;
    }
}
