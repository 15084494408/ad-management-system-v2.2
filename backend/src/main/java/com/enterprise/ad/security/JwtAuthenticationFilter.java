package com.enterprise.ad.security;

import io.jsonwebtoken.Claims;
import com.enterprise.ad.common.util.WebUtil;
import com.enterprise.ad.module.system.user.mapper.SysUserMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器（每个请求只执行一次）
 *
 * ★ 修复 P1-8: 使用 parseClaims() 一次性解析 Token，避免重复解析
 *
 * 核心逻辑：
 * 1. 从 Header 提取 JWT Token
 * 2. 一次性解析出 Claims，提取 userId + username
 * 3. 用 username 加载 UserDetails 并验证 Token
 * 4. 设置 SecurityContext 认证信息
 * 5. 将 userId / username 设置到 request attribute，供 Controller 中 @RequestAttribute 使用
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractToken(request);
            if (token != null) {
                // ★ 修复 P1-8: 一次性解析 Token，不再分别调用 parseUserId + parseUsername
                Claims claims;
                try {
                    claims = jwtUtil.parseClaims(token);
                } catch (Exception e) {
                    log.debug("JWT Token 解析失败: {}", e.getMessage());
                    filterChain.doFilter(request, response);
                    return;
                }

                Long userId = Long.parseLong(claims.getSubject());
                String username = claims.get("username", String.class);

                if (username != null && jwtUtil.validateToken(token, userId)) {
                    // 用 username 加载用户信息
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    // 设置 Spring Security 认证上下文
                    UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    // ★ 将用户信息设置到 request attribute，供 Controller 的 @RequestAttribute 使用
                    request.setAttribute("userId", userId);
                    request.setAttribute("username", username);
                }
            }
        } catch (Exception e) {
            log.error("JWT认证失败: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头中提取 Token（使用 JwtUtil 配置的前缀）
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(jwtUtil.getHeader());
        if (StringUtils.hasText(header) && header.startsWith(jwtUtil.getPrefix() + " ")) {
            return header.substring(jwtUtil.getPrefix().length() + 1);
        }
        return null;
    }
}
