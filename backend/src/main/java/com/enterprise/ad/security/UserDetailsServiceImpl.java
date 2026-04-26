package com.enterprise.ad.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.ad.module.system.user.entity.SysPermission;
import com.enterprise.ad.module.system.user.entity.SysUser;
import com.enterprise.ad.module.system.user.mapper.SysPermissionMapper;
import com.enterprise.ad.module.system.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring Security 用户认证服务
 * 
 * UserDetails.principal 存储真实 username（而非 userId），
 * 这样通过 SecurityContext 获取的 username 可直接用于业务查询。
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserMapper sysUserMapper;
    private final SysPermissionMapper sysPermissionMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 根据用户名查询用户
        SysUser user = sysUserMapper.selectOne(
            new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .eq(SysUser::getDeleted, 0)
        );

        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        if (user.getStatus() != 1) {
            throw new UsernameNotFoundException("用户已被禁用");
        }

        // 查询用户角色
        List<String> roles = sysUserMapper.selectRolesByUserId(user.getId());
        boolean isSuperAdmin = roles.contains("SUPER_ADMIN");

        // 查询权限码
        List<String> permissions;
        if (isSuperAdmin) {
            // SUPER_ADMIN：查询数据库中所有有效的 permission_code
            List<SysPermission> allPermissions = sysPermissionMapper.selectList(
                new LambdaQueryWrapper<SysPermission>()
                    .eq(SysPermission::getStatus, 1)
                    .eq(SysPermission::getDeleted, 0)
                    .isNotNull(SysPermission::getPermissionCode)
                    .ne(SysPermission::getPermissionCode, "")
            );
            permissions = allPermissions.stream()
                .map(SysPermission::getPermissionCode)
                .collect(Collectors.toList());
        } else {
            permissions = sysUserMapper.selectPermissionsByUserId(user.getId());
        }

        // 合并角色和权限（角色加 ROLE_ 前缀）
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.addAll(roles.stream()
            .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
            .collect(Collectors.toList()));
        authorities.addAll(permissions.stream()
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList()));

        return new User(
            user.getUsername(),
            user.getPassword(),
            user.getStatus() == 1,
            true, true, true,
            authorities
        );
    }
}
