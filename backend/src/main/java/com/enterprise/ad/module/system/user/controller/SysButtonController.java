package com.enterprise.ad.module.system.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.ad.common.Result;
import com.enterprise.ad.module.system.user.entity.SysButton;
import com.enterprise.ad.module.system.user.mapper.SysButtonMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import com.enterprise.ad.module.system.user.service.SysButtonService;

/**
 * 按钮权限管理
 */
@RestController
@RequestMapping("/system/buttons")
@RequiredArgsConstructor
@Tag(name = "系统管理 - 按钮管理")
@PreAuthorize("hasAuthority('system:menu')")
public class SysButtonController {

    private final SysButtonMapper buttonMapper;
    private final SysButtonService sysButtonService;

    @GetMapping
    @Operation(summary = "按钮列表（支持树形）")
    public Result<List<SysButton>> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<SysButton> qw = new LambdaQueryWrapper<SysButton>()
            .eq(SysButton::getDeleted, 0)
            .like(name != null, SysButton::getName, name)
            .eq(status != null, SysButton::getStatus, status)
            .orderByAsc(SysButton::getSort);
        List<SysButton> list = sysButtonService.list(qw);
        return Result.ok(list);
    }

    @PostMapping
    @Operation(summary = "新增按钮")
    public Result<Long> create(@RequestBody SysButton button) {
        button.setCreateTime(LocalDateTime.now());
        button.setUpdateTime(LocalDateTime.now());
        if (button.getStatus() == null) button.setStatus(1);
        if (button.getSort() == null) button.setSort(0);
        sysButtonService.save(button);
        return Result.ok(button.getId());
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新按钮")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysButton button) {
        button.setId(id);
        button.setUpdateTime(LocalDateTime.now());
        buttonMapper.updateById(button);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "切换按钮状态")
    public Result<Void> toggleStatus(@PathVariable Long id, @RequestBody java.util.Map<String, Integer> body) {
        SysButton button = new SysButton();
        button.setId(id);
        button.setStatus(body.get("status"));
        button.setUpdateTime(LocalDateTime.now());
        buttonMapper.updateById(button);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除按钮")
    public Result<Void> delete(@PathVariable Long id) {
        // ★ 修复：deleteById 在 @TableLogic 下会自动转为逻辑删除
        sysButtonService.removeById(id);
        return Result.ok();
    }
}
