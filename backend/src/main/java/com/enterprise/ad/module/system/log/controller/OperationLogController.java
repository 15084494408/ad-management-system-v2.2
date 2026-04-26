package com.enterprise.ad.module.system.log.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.ad.common.PageResult;
import com.enterprise.ad.common.Result;
import com.enterprise.ad.module.system.log.entity.OperationLog;
import com.enterprise.ad.module.system.log.mapper.OperationLogMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/system/logs")
@RequiredArgsConstructor
@Tag(name = "系统管理 - 操作日志")
@PreAuthorize("hasAuthority('system:log')")
public class OperationLogController {

    private final OperationLogMapper logMapper;

    @GetMapping
    @Operation(summary = "日志列表（分页）")
    public Result<PageResult<OperationLog>> list(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        
        Page<OperationLog> page = new Page<>(current, size);
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(23, 59, 59) : null;
        LambdaQueryWrapper<OperationLog> qw = new LambdaQueryWrapper<OperationLog>()
            .eq(OperationLog::getDeleted, 0)
            .like(username != null, OperationLog::getUsername, username)
            .eq(module != null, OperationLog::getModule, module)
            .ge(startDateTime != null, OperationLog::getCreateTime, startDateTime)
            .le(endDateTime != null, OperationLog::getCreateTime, endDateTime)
            .orderByDesc(OperationLog::getCreateTime);
        
        Page<OperationLog> result = logMapper.selectPage(page, qw);
        return Result.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "日志详情")
    public Result<OperationLog> getById(@PathVariable Long id) {
        return Result.ok(logMapper.selectById(id));
    }

    @GetMapping("/modules")
    @Operation(summary = "获取所有模块")
    public Result<?> getModules() {
        return Result.ok(java.util.List.of("用户管理", "订单管理", "财务管理", "客户管理", "物料管理", "系统配置"));
    }

    @GetMapping("/export")
    @Operation(summary = "导出操作日志")
    public void exportLogs(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_OK);
    }
}
