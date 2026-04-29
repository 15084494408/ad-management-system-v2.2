package com.enterprise.ad.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import com.enterprise.ad.common.util.WebUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 登录接口速率限制过滤器
 * ★ 修复 P0-3: 基于 Redis 的滑动窗口计数，防止暴力破解
 * 规则：同一用户名 5 次失败后锁定 15 分钟；同一 IP 每分钟最多 20 次尝试
 */
@Slf4j
@Component
public class RateLimiterFilter extends OncePerRequestFilter {

    private static final int MAX_FAIL_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 15;
    private static final int MAX_IP_PER_MINUTE = 20;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RateLimiterFilter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 只拦截登录接口
        if (!"/auth/login".equals(request.getServletPath()) || !"POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = WebUtil.getClientIp(request);

        // 检查 IP 级别速率限制
        String ipKey = "rate:login:ip:" + clientIp;
        String ipCount = redisTemplate.opsForValue().get(ipKey);
        if (ipCount != null && Integer.parseInt(ipCount) >= MAX_IP_PER_MINUTE) {
            log.warn("登录速率限制触发 - IP: {} 在一分钟内尝试次数过多", clientIp);
            sendRateLimitResponse(response, "操作过于频繁，请稍后再试");
            return;
        }

        // 递增 IP 计数
        Long newIpCount = redisTemplate.opsForValue().increment(ipKey);
        if (newIpCount != null && newIpCount == 1) {
            redisTemplate.expire(ipKey, 1, TimeUnit.MINUTES);
        }

        // 继续过滤器链
        filterChain.doFilter(request, response);

        // 检查响应状态，如果是 401 则增加用户名失败计数
        if (response.getStatus() == 401) {
            // 尝试从请求体中提取用户名（仅在流未消费时）
            // 如果无法提取，则跳过用户名级别限制（IP 级别仍然生效）
        }
    }

    /**
     * 记录登录失败（由 AuthServiceImpl 调用）
     */
    public void recordLoginFailure(String username) {
        if (username == null || username.isBlank()) return;

        String key = "rate:login:fail:" + username;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null) {
            if (count == 1) {
                redisTemplate.expire(key, LOCK_DURATION_MINUTES, TimeUnit.MINUTES);
            }
            if (count >= MAX_FAIL_ATTEMPTS) {
                log.warn("用户 {} 登录失败 {} 次，已锁定 {} 分钟", username, count, LOCK_DURATION_MINUTES);
            }
        }
    }

    /**
     * 检查用户名是否被锁定
     */
    public boolean isUsernameLocked(String username) {
        if (username == null || username.isBlank()) return false;

        String key = "rate:login:fail:" + username;
        String count = redisTemplate.opsForValue().get(key);
        if (count != null && Integer.parseInt(count) >= MAX_FAIL_ATTEMPTS) {
            Long ttl = redisTemplate.getExpire(key, TimeUnit.MINUTES);
            return ttl != null && ttl > 0;
        }
        return false;
    }

    /**
     * 登录成功后清除失败计数
     */
    public void clearLoginFailure(String username) {
        if (username == null || username.isBlank()) return;
        redisTemplate.delete("rate:login:fail:" + username);
    }

    /**
     * 获取剩余锁定时间（分钟）
     */
    public long getRemainingLockMinutes(String username) {
        if (username == null || username.isBlank()) return 0;
        String key = "rate:login:fail:" + username;
        Long ttl = redisTemplate.getExpire(key, TimeUnit.MINUTES);
        return ttl != null ? Math.max(ttl, 0) : 0;
    }

    private void sendRateLimitResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        Map<String, Object> body = Map.of("code", 429, "message", message);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
