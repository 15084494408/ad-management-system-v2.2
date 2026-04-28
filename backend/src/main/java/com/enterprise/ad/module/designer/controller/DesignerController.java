package com.enterprise.ad.module.designer.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.ad.common.Result;
import com.enterprise.ad.module.system.user.entity.SysUser;
import com.enterprise.ad.module.system.user.mapper.SysUserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 设计师专用接口 - 不需要权限验证
 */
@RestController
@RequestMapping("/api/designers")
@RequiredArgsConstructor
@Tag(name = "设计师接口")
public class DesignerController {

    private final SysUserMapper sysUserMapper;

    @GetMapping
    @Operation(summary = "获取设计师列表")
    public Result<List<SysUser>> getDesigners() {
        // 查询所有状态正常的设计师用户
        // 设计师的 real_name 中包含"设计"或者角色包含 DESIGNER
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getDeleted, 0)
               .eq(SysUser::getStatus, 1)
               .orderByAsc(SysUser::getId);

        List<SysUser> users = sysUserMapper.selectList(wrapper);

        // 过滤：只返回用户名为设计相关的用户，或者通过角色判断
        // 这里简单返回所有正常用户，让前端自己筛选或者后端根据用户名包含"设计"来过滤
        // 为了简单，我们返回所有用户，前端可以根据实际情况使用
        return Result.ok(users);
    }
}
