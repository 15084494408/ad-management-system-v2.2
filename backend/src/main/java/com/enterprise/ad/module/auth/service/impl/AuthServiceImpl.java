package com.enterprise.ad.module.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.ad.common.exception.BusinessException;
import com.enterprise.ad.config.RateLimiterFilter;
import com.enterprise.ad.module.auth.dto.LoginRequest;
import com.enterprise.ad.module.auth.dto.LoginResponse;
import com.enterprise.ad.module.system.user.entity.SysUser;
import com.enterprise.ad.module.system.user.mapper.SysUserMapper;
import com.enterprise.ad.security.JwtUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements com.enterprise.ad.module.auth.service.AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final SysUserMapper sysUserMapper;
    private final RateLimiterFilter rateLimiterFilter;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public LoginResponse login(LoginRequest request) {
        // ★ 修复 P0-3: 检查用户名是否被锁定
        if (rateLimiterFilter.isUsernameLocked(request.getUsername())) {
            long remaining = rateLimiterFilter.getRemainingLockMinutes(request.getUsername());
            throw new BusinessException(429, "登录失败次数过多，账号已锁定，请 " + remaining + " 分钟后再试");
        }

        try {
            // Spring Security 认证
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 查询用户信息
            SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUsername, request.getUsername())
                    .eq(SysUser::getDeleted, 0)
            );

            if (user == null || user.getStatus() != 1) {
                rateLimiterFilter.recordLoginFailure(request.getUsername());
                throw new BusinessException("账号已被禁用");
            }

            // 生成JWT Token
            String token = jwtUtil.generateToken(user.getId(), user.getUsername());

            // 查询角色和权限
            List<String> roles = sysUserMapper.selectRolesByUserId(user.getId());
            List<String> permissions = sysUserMapper.selectPermissionsByUserId(user.getId());

            // ★ 登录成功，清除失败计数
            rateLimiterFilter.clearLoginFailure(request.getUsername());

            LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                user.getId(), user.getUsername(), user.getRealName(),
                user.getAvatar(), roles, permissions
            );

            return new LoginResponse(token, userInfo);

        } catch (BadCredentialsException e) {
            // ★ 记录失败次数
            rateLimiterFilter.recordLoginFailure(request.getUsername());
            long remaining = rateLimiterFilter.getRemainingLockMinutes(request.getUsername());
            int failedCount = Math.min(5, 5); // 隐晦不暴露具体失败次数
            throw new BusinessException(401, "用户名或密码错误");
        }
    }

    @Override
    public void logout(String token) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
            String username = userDetails.getUsername();
            SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUsername, username)
                    .eq(SysUser::getDeleted, 0)
            );
            if (user != null) {
                jwtUtil.invalidateToken(token, user.getId());
            }
        }
        SecurityContextHolder.clearContext();
    }

    @Override
    public LoginResponse.UserInfo getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(401, "未登录");
        }

        if (!(authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails userDetails)) {
            throw new BusinessException(401, "用户信息无效");
        }

        String username = userDetails.getUsername();
        String cacheKey = "user:info:" + username;

        // ★ 修复 P2-17: 使用 Redis 缓存用户信息，避免每次 3 次数据库查询
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return objectMapper.readValue(cached, LoginResponse.UserInfo.class);
            }
        } catch (Exception e) {
            // 缓存读取失败不影响主流程
        }

        SysUser user = sysUserMapper.selectOne(
            new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .eq(SysUser::getDeleted, 0)
        );
        if (user == null || user.getDeleted() == 1) {
            throw new BusinessException(401, "用户不存在");
        }

        List<String> roles = sysUserMapper.selectRolesByUserId(user.getId());
        List<String> permissions = sysUserMapper.selectPermissionsByUserId(user.getId());

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
            user.getId(), user.getUsername(), user.getRealName(),
            user.getAvatar(), roles, permissions
        );

        // 写入缓存（5分钟）
        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(userInfo), 5, TimeUnit.MINUTES);
        } catch (Exception ignored) {
        }

        return userInfo;
    }
}
