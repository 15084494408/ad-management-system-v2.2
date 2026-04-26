package com.enterprise.ad.module.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.ad.common.exception.BusinessException;
import com.enterprise.ad.module.auth.dto.LoginRequest;
import com.enterprise.ad.module.auth.dto.LoginResponse;
import com.enterprise.ad.module.system.user.entity.SysUser;
import com.enterprise.ad.module.system.user.mapper.SysUserMapper;
import com.enterprise.ad.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements com.enterprise.ad.module.auth.service.AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final SysUserMapper sysUserMapper;

    @Override
    public LoginResponse login(LoginRequest request) {
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
                throw new BusinessException("账号已被禁用");
            }

            // 生成JWT Token
            String token = jwtUtil.generateToken(user.getId(), user.getUsername());

            // 查询角色和权限
            List<String> roles = sysUserMapper.selectRolesByUserId(user.getId());
            List<String> permissions = sysUserMapper.selectPermissionsByUserId(user.getId());

            LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                user.getId(), user.getUsername(), user.getRealName(),
                user.getAvatar(), roles, permissions
            );

            return new LoginResponse(token, userInfo);

        } catch (BadCredentialsException e) {
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

        return new LoginResponse.UserInfo(
            user.getId(), user.getUsername(), user.getRealName(),
            user.getAvatar(), roles, permissions
        );
    }
}
