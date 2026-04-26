package com.enterprise.ad.module.system.role.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.ad.common.Result;
import com.enterprise.ad.module.system.role.entity.Role;
import com.enterprise.ad.module.system.role.entity.RoleVO;
import com.enterprise.ad.module.system.role.mapper.RoleMapper;
import com.enterprise.ad.module.system.user.entity.SysPermission;
import com.enterprise.ad.module.system.user.mapper.SysPermissionMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/system/roles")
@RequiredArgsConstructor
@Tag(name = "角色管理")
public class RoleController {

    private final RoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;

    /**
     * 角色列表（含权限码）
     */
    @GetMapping
    @Operation(summary = "角色列表")
    @PreAuthorize("hasAuthority('system:role')")
    public Result<List<RoleVO>> list() {
        List<Role> roles = roleMapper.selectList(
            new LambdaQueryWrapper<Role>()
                .eq(Role::getDeleted, 0)
                .orderByAsc(Role::getSort)
        );
        List<RoleVO> voList = roles.stream().map(this::toVO).collect(Collectors.toList());
        return Result.ok(voList);
    }

    @GetMapping("/{id}")
    @Operation(summary = "角色详情")
    @PreAuthorize("hasAuthority('system:role')")
    public Result<RoleVO> getById(@PathVariable Long id) {
        Role role = roleMapper.selectById(id);
        return Result.ok(toVO(role));
    }

    @PostMapping
    @Operation(summary = "新增角色")
    @PreAuthorize("hasAuthority('system:role')")
    public Result<Long> create(@RequestBody Role role) {
        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());
        role.setDeleted(0);
        roleMapper.insert(role);
        return Result.ok(role.getId());
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新角色")
    @PreAuthorize("hasAuthority('system:role')")
    public Result<Void> update(@PathVariable Long id, @RequestBody Role role) {
        role.setId(id);
        role.setUpdateTime(LocalDateTime.now());
        roleMapper.updateById(role);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除角色")
    @PreAuthorize("hasAuthority('system:role')")
    public Result<Void> delete(@PathVariable Long id) {
        Role role = new Role();
        role.setId(id);
        role.setDeleted(1);
        role.setUpdateTime(LocalDateTime.now());
        roleMapper.updateById(role);
        return Result.ok();
    }

    /**
     * 获取所有权限码（供角色编辑弹窗使用）
     */
    @GetMapping("/permissions/all")
    @Operation(summary = "所有权限码列表")
    @PreAuthorize("hasAuthority('system:role')")
    public Result<List<String>> allPermissions() {
        List<SysPermission> list = permissionMapper.selectList(
            new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getStatus, 1)
                .eq(SysPermission::getDeleted, 0)
                .isNotNull(SysPermission::getPermissionCode)
                .ne(SysPermission::getPermissionCode, "")
                .orderByAsc(SysPermission::getSort)
        );
        List<String> codes = list.stream()
            .map(SysPermission::getPermissionCode)
            .distinct()
            .collect(Collectors.toList());
        return Result.ok(codes);
    }

    private RoleVO toVO(Role role) {
        if (role == null) return null;
        RoleVO vo = new RoleVO();
        vo.setId(role.getId());
        vo.setRoleName(role.getRoleName());
        vo.setRoleCode(role.getRoleCode());
        vo.setDescription(role.getDescription());
        vo.setSort(role.getSort());
        vo.setStatus(role.getStatus());

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        if (role.getCreateTime() != null) vo.setCreateTime(role.getCreateTime().format(fmt));
        if (role.getUpdateTime() != null) vo.setUpdateTime(role.getUpdateTime().format(fmt));

        // 查询角色关联的权限码
        // 使用 RoleMapper 本身的 BaseMapper 查不到跨表数据，
        // 所以用原生 SQL 通过 permissionMapper 查
        List<String> perms;
        if (role.getId() != null) {
            // SUPER_ADMIN 直接返回所有权限码
            if ("SUPER_ADMIN".equals(role.getRoleCode())) {
                List<SysPermission> allPerms = permissionMapper.selectList(
                    new LambdaQueryWrapper<SysPermission>()
                        .eq(SysPermission::getStatus, 1)
                        .eq(SysPermission::getDeleted, 0)
                        .isNotNull(SysPermission::getPermissionCode)
                        .ne(SysPermission::getPermissionCode, "")
                );
                perms = allPerms.stream().map(SysPermission::getPermissionCode).collect(Collectors.toList());
            } else {
                // 从 sys_role_permission + sys_permission 联查
                perms = getPermissionsByRoleId(role.getId());
            }
        } else {
            perms = Collections.emptyList();
        }
        vo.setPermissions(perms);
        return vo;
    }

    /**
     * 根据角色ID查询关联的权限码
     */
    private List<String> getPermissionsByRoleId(Long roleId) {
        // 通过 MyBatis-Plus 的 selectList 无法直接跨表联查，
        // 这里用 SysPermission 的 selectList + 手动过滤
        // 实际上需要 SQL 联查，所以加一个方法到 RoleMapper
        return roleMapper.selectPermissionsByRoleId(roleId);
    }
}
