package com.enterprise.ad.module.system.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.ad.common.PageResult;
import com.enterprise.ad.common.Result;
import com.enterprise.ad.common.SysConstants;
import com.enterprise.ad.module.system.user.entity.SysUser;
import com.enterprise.ad.module.system.user.mapper.SysUserMapper;
import com.enterprise.ad.module.system.user.dto.CreateUserDTO;
import com.enterprise.ad.module.system.user.dto.UpdateUserDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import com.enterprise.ad.module.system.user.service.SysUserService;

@RestController
@RequestMapping("/system/users")
@RequiredArgsConstructor
@Tag(name = "系统管理 - 用户")
@PreAuthorize("hasAuthority('system:user')")
public class SysUserController {

    private final SysUserMapper sysUserMapper;
    private final SysUserService sysUserService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    @Operation(summary = "用户列表（分页）")
    public Result<PageResult<SysUser>> list(
            @RequestParam(defaultValue = "1") long current,
            // ★ 修复 P2-9: 分页上限校验
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String realName,
            @RequestParam(required = false) Integer roleId,
            @RequestParam(required = false) Integer status) {
        // ★ 分页上限校验
        size = Math.min(size, 100);
        Page<SysUser> page = new Page<>(current, size);
        LambdaQueryWrapper<SysUser> qw = new LambdaQueryWrapper<SysUser>()
            .eq(SysUser::getDeleted, 0)
            .like(username != null, SysUser::getUsername, username)
            .like(realName != null, SysUser::getRealName, realName)
            .eq(status != null, SysUser::getStatus, status)
            .orderByDesc(SysUser::getCreateTime);

        // 如果按角色筛选，先查该角色下的用户ID，再过滤
        if (roleId != null) {
            List<Number> userIdNums = sysUserMapper.selectUserRoleIdsByRoleId(roleId.longValue());
            if (userIdNums.isEmpty()) {
                // 该角色下无用户，直接返回空
                return Result.ok(PageResult.of(0, current, size, Collections.emptyList()));
            }
            List<Long> filteredUserIds = userIdNums.stream().map(Number::longValue).collect(Collectors.toList());
            qw.in(SysUser::getId, filteredUserIds);
        }

        Page<SysUser> result = sysUserService.page(page, qw);

        // 批量填充用户的角色ID列表
        List<SysUser> records = result.getRecords();
        if (!records.isEmpty()) {
            List<Long> userIds = records.stream().map(SysUser::getId).collect(Collectors.toList());
            List<Map<String, Object>> userRoles = sysUserMapper.selectUserRoleIds(userIds);
            // 按用户ID分组
            Map<Long, List<Long>> roleIdMap = new HashMap<>();
            for (Map<String, Object> ur : userRoles) {
                Long uid = ((Number) ur.get("user_id")).longValue();
                Long rid = ((Number) ur.get("role_id")).longValue();
                roleIdMap.computeIfAbsent(uid, k -> new ArrayList<>()).add(rid);
            }
            for (SysUser u : records) {
                u.setRoleIds(roleIdMap.getOrDefault(u.getId(), Collections.emptyList()));
            }
        }

        return Result.ok(PageResult.of(result.getTotal(), result.getCurrent(),
            result.getSize(), records));
    }

    @PostMapping
    @Operation(summary = "新建用户")
    @Transactional
    public Result<Void> create(@Valid @RequestBody CreateUserDTO dto) {
        // ★ 修复 P0-1: 使用 DTO 接收请求，避免前端传入非法字段
        // 检查用户名唯一
        long count = sysUserService.count(
            new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, dto.getUsername())
                .eq(SysUser::getDeleted, 0));
        if (count > 0) {
            return Result.fail(400, "用户名已存在");
        }

        // 将 DTO 转换为 Entity
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        // ★ 修复 P2-8: 默认密码从配置常量读取，不再硬编码 "123456"
        user.setPassword(passwordEncoder.encode(dto.getPassword() != null ? dto.getPassword() : SysConstants.DEFAULT_PASSWORD));
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setAvatar(dto.getAvatar());
        user.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        user.setCreateTime(LocalDateTime.now());
        user.setDeleted(0);

        sysUserService.save(user);

        // 分配角色
        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
            for (Long roleId : dto.getRoleIds()) {
                sysUserMapper.insertUserRole(user.getId(), roleId);
            }
        }
        return Result.ok("用户创建成功", null);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新用户")
    @Transactional
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UpdateUserDTO dto) {
        // ★ 修复 P0-1: 使用 DTO 接收请求，避免前端传入非法字段
        SysUser existing = sysUserService.getById(id);
        if (existing == null || existing.getDeleted() == 1) {
            return Result.fail("用户不存在");
        }

        // 将 DTO 的非空字段更新到 Entity
        SysUser update = new SysUser();
        update.setId(id);
        update.setUpdateTime(LocalDateTime.now());

        if (dto.getRealName() != null) update.setRealName(dto.getRealName());
        if (dto.getPhone() != null) update.setPhone(dto.getPhone());
        if (dto.getEmail() != null) update.setEmail(dto.getEmail());
        if (dto.getAvatar() != null) update.setAvatar(dto.getAvatar());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());
        // 注意：不更新密码字段，密码修改需要单独接口

        sysUserMapper.updateById(update);

        // 更新角色分配
        if (dto.getRoleIds() != null) {
            sysUserMapper.deleteUserRole(id);
            for (Long roleId : dto.getRoleIds()) {
                sysUserMapper.insertUserRole(id, roleId);
            }
        }
        return Result.ok();
    }

    @PutMapping("/{id}/password")
    @Operation(summary = "重置密码")
    public Result<Void> resetPassword(@PathVariable Long id) {
        // ★ 修复 P1-1: 添加存在性校验
        SysUser existing = sysUserService.getById(id);
        if (existing == null || existing.getDeleted() == 1) {
            return Result.fail("用户不存在");
        }

        SysUser user = new SysUser();
        user.setId(id);
        // ★ 修复 P2-8: 默认密码从配置常量读取，不再硬编码
        user.setPassword(passwordEncoder.encode(SysConstants.DEFAULT_PASSWORD));
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.updateById(user);
        return Result.ok("密码已重置", null);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "启用/禁用用户")
    @Transactional
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        // ★ 修复 P1-1: 添加存在性校验
        SysUser existing = sysUserService.getById(id);
        if (existing == null || existing.getDeleted() == 1) {
            return Result.fail("用户不存在");
        }

        SysUser user = new SysUser();
        user.setId(id);
        user.setStatus(status);
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.updateById(user);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户")
    public Result<Void> delete(@PathVariable Long id) {
        // ★ 修复：deleteById 在 @TableLogic 下会自动转为逻辑删除
        sysUserService.removeById(id);
        return Result.ok();
    }

    @GetMapping("/roles/{userId}")
    @Operation(summary = "获取用户角色")
    public Result<List<String>> getUserRoles(@PathVariable Long userId) {
        List<String> roles = sysUserMapper.selectRolesByUserId(userId);
        return Result.ok(roles);
    }

    @PutMapping("/{id}/roles")
    @Operation(summary = "分配角色")
    @Transactional  // ★ 修复：添加事务保证原子性
    public Result<Void> assignRoles(@PathVariable Long id, @RequestBody List<Long> roleIds) {
        sysUserMapper.deleteUserRole(id);
        for (Long roleId : roleIds) {
            sysUserMapper.insertUserRole(id, roleId);
        }
        return Result.ok();
    }

    @GetMapping("/designers")
    @Operation(summary = "获取设计师列表")
    public Result<List<SysUser>> getDesigners() {
        List<SysUser> designers = sysUserMapper.selectUsersByRoleCode("DESIGNER");
        return Result.ok(designers);
    }

    @GetMapping("/designer-list")
    @Operation(summary = "获取设计师列表（无需权限）")
    @PreAuthorize("permitAll()")
    public Result<List<SysUser>> getDesignerList() {
        List<SysUser> designers = sysUserMapper.selectUsersByRoleCode("DESIGNER");
        return Result.ok(designers);
    }
}
