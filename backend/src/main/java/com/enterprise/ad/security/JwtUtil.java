package com.enterprise.ad.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * JWT 工具类
 * ★ 修复 P1-8: 新增 parseClaims() 一次性解析方法，避免重复解析
 * ★ 修复 P1-9: 密钥不足 256bit 时拒绝启动，不再用 0 填充
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${jwt.header}")
    private String header;

    @Value("${jwt.prefix}")
    private String prefix;

    private SecretKey secretKey;

    private final RedisTemplate<String, Object> redisTemplate;

    public JwtUtil(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void init() {
        // ★ 修复 P1-9: 密钥不足 32 字节直接拒绝启动，不再用 0 填充
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                "JWT secret must be at least 256 bits (32 bytes) for HMAC-SHA256. " +
                "Current length: " + keyBytes.length + " bytes. " +
                "Set a longer secret via the JWT_SECRET environment variable."
            );
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        log.info("JWT 密钥初始化完成，密钥长度: {} bytes", keyBytes.length);
    }

    /**
     * 生成 Token
     */
    public String generateToken(Long userId, String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        String token = Jwts.builder()
            .subject(userId.toString())
            .claim("username", username)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey)
            .compact();

        // 将 Token 存入 Redis（用于退出登录时拉黑）
        String redisKey = "jwt:token:" + userId;
        redisTemplate.opsForValue().set(redisKey, token, expiration, TimeUnit.MILLISECONDS);

        return prefix + token;
    }

    /**
     * ★ 修复 P1-8: 一次性解析 Token，返回 Claims
     * 避免每个请求调用两次 parseSignedClaims
     */
    public Claims parseClaims(String token) {
        if (token != null && token.startsWith(prefix)) {
            token = token.substring(prefix.length());
        }
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    /**
     * 解析 Token，获取用户ID（保留向后兼容）
     */
    public Long parseUserId(String token) {
        try {
            Claims claims = parseClaims(token);
            return Long.parseLong(claims.getSubject());
        } catch (ExpiredJwtException e) {
            log.warn("Token 已过期: {}", e.getMessage());
            return null;
        } catch (JwtException e) {
            log.warn("Token 无效: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 解析 Token，获取用户名（保留向后兼容）
     */
    public String parseUsername(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.get("username", String.class);
        } catch (JwtException e) {
            log.warn("Token 无效: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 验证 Token 是否有效（检查Redis黑名单）
     */
    public boolean validateToken(String token, Long userId) {
        Long userIdFromToken = parseUserId(token);
        if (userIdFromToken == null || !userIdFromToken.equals(userId)) {
            return false;
        }
        // 检查是否在黑名单中
        String blackKey = "jwt:blacklist:" + token;
        return redisTemplate.hasKey(blackKey) == Boolean.FALSE;
    }

    /**
     * 退出登录（将 Token 加入黑名单）
     */
    public void invalidateToken(String token, Long userId) {
        if (token.startsWith(prefix)) {
            token = token.substring(prefix.length());
        }
        String blackKey = "jwt:blacklist:" + token;
        // 黑名单有效期 = Token 剩余有效期
        long ttl = getRemainingTime(token);
        if (ttl > 0) {
            redisTemplate.opsForValue().set(blackKey, "1", ttl, TimeUnit.MILLISECONDS);
        }
        // 删除用户Token记录
        String tokenKey = "jwt:token:" + userId;
        redisTemplate.delete(tokenKey);
    }

    /**
     * 获取 Token 剩余有效时间（秒）
     */
    public long getRemainingTime(String token) {
        try {
            Claims claims = parseClaims(token);
            Date expiration = claims.getExpiration();
            long remaining = (expiration.getTime() - System.currentTimeMillis()) / 1000;
            return Math.max(remaining, 0);
        } catch (Exception e) {
            return 0;
        }
    }

    public String getHeader() {
        return header;
    }

    public String getPrefix() {
        return prefix;
    }
}
