package com.enterprise.ad.module.designer.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.ad.common.Result;
import com.enterprise.ad.module.system.user.entity.SysUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.enterprise.ad.module.system.user.service.SysUserService;

/**
 * 设计师专用接口 - 需要登录权限
 */
@RestController
@RequestMapping("/api/designers")
@RequiredArgsConstructor
@Tag(name = "设计师接口")
public class DesignerController {

    private final SysUserService sysUserService;

    @GetMapping
    @Operation(summary = "获取设计师列表")
    @PreAuthorize("isAuthenticated()")
    public Result<List<Map<String, Object>>> getDesigners() {
        // ★ P1-04 修复：添加权限控制，且只返回必要字段（不暴露手机号、邮箱等敏感信息）
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getDeleted, 0)
               .eq(SysUser::getStatus, 1)
               .orderByAsc(SysUser::getId);

        List<SysUser> users = sysUserService.list(wrapper);

        // 只返回 id, realName, username 字段，不暴露 phone/email 等敏感信息
        List<Map<String, Object>> result = users.stream().map(u -> {
            Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("id", u.getId());
            item.put("realName", u.getRealName());
            item.put("username", u.getUsername());
            return item;
        }).collect(Collectors.toList());

        return Result.ok(result);
    }
}
