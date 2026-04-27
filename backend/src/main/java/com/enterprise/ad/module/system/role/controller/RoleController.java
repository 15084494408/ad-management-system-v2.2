package com.enterprise.ad.module.system.role.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.ad.common.Result;
import com.enterprise.ad.module.system.role.entity.Role;
import com.enterprise.ad.module.system.role.entity.RoleRequest;
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
    public Result<Long> create(@RequestBody RoleRequest req) {
        Role role = new Role();
        role.setRoleName(req.getRoleName());
        role.setRoleCode(req.getRoleCode());
        role.setDescription(req.getDescription());
        role.setSort(req.getSort());
        role.setStatus(1);
        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());
        role.setDeleted(0);
        roleMapper.insert(role);

        // 同步权限到 sys_role_permission
        syncRolePermissions(role.getId(), req.getPermissions());

        return Result.ok(role.getId());
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新角色")
    @PreAuthorize("hasAuthority('system:role')")
    public Result<Void> update(@PathVariable Long id, @RequestBody RoleRequest req) {
        Role role = new Role();
        role.setId(id);
        role.setRoleName(req.getRoleName());
        role.setDescription(req.getDescription());
        role.setSort(req.getSort());
        role.setUpdateTime(LocalDateTime.now());
        roleMapper.updateById(role);

        // 同步权限到 sys_role_permission
        syncRolePermissions(id, req.getPermissions());

        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除角色")
    @PreAuthorize("hasAuthority('system:role')")
    public Result<Void> delete(@PathVariable Long id) {
        // ★ 修复：deleteById 在 @TableLogic 下会自动转为逻辑删除
        roleMapper.deleteById(id);
        // 同时清理权限关联
        roleMapper.deletePermissionsByRoleId(id);
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
        return roleMapper.selectPermissionsByRoleId(roleId);
    }

    /**
     * 同步角色权限：先删除旧的，再根据权限码列表查出ID批量插入
     */
    private void syncRolePermissions(Long roleId, List<String> permissionCodes) {
        // 先清除旧关联
        roleMapper.deletePermissionsByRoleId(roleId);

        if (permissionCodes == null || permissionCodes.isEmpty()) {
            return;
        }

        // 根据权限码查出对应的 permission_id
        List<SysPermission> perms = permissionMapper.selectList(
            new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getStatus, 1)
                .eq(SysPermission::getDeleted, 0)
                .in(SysPermission::getPermissionCode, permissionCodes)
        );

        List<Long> permissionIds = perms.stream()
            .map(SysPermission::getId)
            .distinct()
            .collect(Collectors.toList());

        if (!permissionIds.isEmpty()) {
            roleMapper.batchInsertPermissions(roleId, permissionIds);
        }
    }
}
