package com.enterprise.ad.module.system.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.ad.common.PageResult;
import com.enterprise.ad.common.Result;
import com.enterprise.ad.module.system.user.entity.SysUser;
import com.enterprise.ad.module.system.user.mapper.SysUserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/system/users")
@RequiredArgsConstructor
@Tag(name = "系统管理 - 用户")
@PreAuthorize("hasAuthority('system:user')")
public class SysUserController {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    @Operation(summary = "用户列表（分页）")
    public Result<PageResult<SysUser>> list(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String realName) {
        Page<SysUser> page = new Page<>(current, size);
        LambdaQueryWrapper<SysUser> qw = new LambdaQueryWrapper<SysUser>()
            .eq(SysUser::getDeleted, 0)
            .like(username != null, SysUser::getUsername, username)
            .like(realName != null, SysUser::getRealName, realName)
            .orderByDesc(SysUser::getCreateTime);
        Page<SysUser> result = sysUserMapper.selectPage(page, qw);
        return Result.ok(PageResult.of(result.getTotal(), result.getCurrent(),
            result.getSize(), result.getRecords()));
    }

    @PostMapping
    @Operation(summary = "新建用户")
    public Result<Void> create(@RequestBody SysUser user) {
        // 检查用户名唯一
        long count = sysUserMapper.selectCount(
            new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, user.getUsername())
                .eq(SysUser::getDeleted, 0));
        if (count > 0) {
            return Result.fail(400, "用户名已存在");
        }
        user.setPassword(passwordEncoder.encode("123456")); // 默认密码
        user.setCreateTime(LocalDateTime.now());
        if (user.getStatus() == null) user.setStatus(1);
        sysUserMapper.insert(user);
        return Result.ok("用户创建成功，默认密码123456", null);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新用户")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysUser user) {
        user.setId(id);
        user.setPassword(null);  // 不允许通过此接口修改密码
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.updateById(user);
        return Result.ok();
    }

    @PutMapping("/{id}/password")
    @Operation(summary = "重置密码")
    public Result<Void> resetPassword(@PathVariable Long id) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setPassword(passwordEncoder.encode("123456"));
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.updateById(user);
        return Result.ok("密码已重置为123456", null);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "启用/禁用用户")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
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
        sysUserMapper.deleteById(id);
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
}
