package com.enterprise.ad.common.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Web 公共工具类
 * ★ 抽取自 RateLimiterFilter / OperationLogAspect / JwtAuthenticationFilter 的重复代码
 */
public final class WebUtil {

    private WebUtil() {}

    /**
     * 获取客户端真实 IP（支持反向代理）
     * 规则：X-Forwarded-For > X-Real-IP > RemoteAddr，多级代理取第一个
     */
    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 从请求头中提取当前登录用户 ID
     * 要求：JwtAuthenticationFilter 已将 userId 设置到 request attribute
     */
    public static Long getCurrentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        return userId != null ? (Long) userId : null;
    }

    /**
     * 从请求头中提取当前登录用户名
     */
    public static String getCurrentUsername(HttpServletRequest request) {
        Object username = request.getAttribute("username");
        return username != null ? username.toString() : null;
    }
}
