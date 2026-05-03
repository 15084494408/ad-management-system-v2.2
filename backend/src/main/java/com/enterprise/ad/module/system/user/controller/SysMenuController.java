package com.enterprise.ad.module.system.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.ad.common.Result;
import com.enterprise.ad.module.system.user.entity.SysPermission;
import com.enterprise.ad.module.system.user.mapper.SysPermissionMapper;
import com.enterprise.ad.security.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.enterprise.ad.module.system.user.service.SysPermissionService;

/**
 * 动态菜单API（前端根据此数据动态渲染侧边栏）
 */
@RestController
@RequestMapping("/system/menus")
@RequiredArgsConstructor
@Tag(name = "系统管理 - 菜单")
public class SysMenuController {

    private final SysPermissionMapper sysPermissionMapper;
    private final SysPermissionService sysPermissionService;
    private final JwtUtil jwtUtil;

    @GetMapping
    @Operation(summary = "获取当前用户菜单树")
    public Result<List<SysPermission>> getCurrentUserMenus() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            return Result.ok(List.of());
        }
        Long userId = Long.parseLong(auth.getPrincipal().toString());
        List<SysPermission> menus = sysPermissionMapper.selectMenuTreeByUserId(userId);
        // 转换为树形结构
        List<SysPermission> tree = buildTree(menus);
        return Result.ok(tree);
    }

    @GetMapping("/all")
    @Operation(summary = "获取所有菜单（系统配置用）")
    public Result<List<SysPermission>> getAllMenus() {
        List<SysPermission> list = sysPermissionService.list(
            new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getDeleted, 0)
                .orderByAsc(SysPermission::getSort)
        );
        return Result.ok(buildTree(list));
    }

    /**
     * 递归构建菜单树
     */
    private List<SysPermission> buildTree(List<SysPermission> list) {
        return list.stream()
            .filter(m -> m.getParentId() == 0)
            .peek(m -> m.setChildren(getChildren(m.getId(), list)))
            .toList();
    }

    private List<SysPermission> getChildren(Long parentId, List<SysPermission> list) {
        return list.stream()
            .filter(m -> parentId.equals(m.getParentId()))
            .peek(m -> m.setChildren(getChildren(m.getId(), list)))
            .toList();
    }
}
