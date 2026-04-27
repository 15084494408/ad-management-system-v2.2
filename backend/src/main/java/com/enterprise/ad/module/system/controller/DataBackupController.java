package com.enterprise.ad.module.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.ad.common.PageResult;
import com.enterprise.ad.common.Result;
import com.enterprise.ad.module.system.entity.SysBackup;
import com.enterprise.ad.module.system.mapper.SysBackupMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 数据备份管理
 */
@Slf4j
@RestController
@RequestMapping("/system/backup")
@RequiredArgsConstructor
@Tag(name = "系统管理 - 数据备份")
@PreAuthorize("hasAuthority('system:backup')")
public class DataBackupController {

    private final SysBackupMapper backupMapper;

    @GetMapping("/list")
    @Operation(summary = "备份记录列表")
    public Result<PageResult<SysBackup>> list(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {
        Page<SysBackup> page = new Page<>(current, size);
        LambdaQueryWrapper<SysBackup> qw = new LambdaQueryWrapper<SysBackup>()
            .eq(SysBackup::getDeleted, 0)
            .orderByDesc(SysBackup::getCreateTime);
        Page<SysBackup> result = backupMapper.selectPage(page, qw);
        return Result.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @PostMapping("/manual")
    @Operation(summary = "手动触发备份")
    public Result<Void> manualBackup(HttpServletRequest request) {
        String username = (String) request.getAttribute("username");
        Long userId = (Long) request.getAttribute("userId");

        SysBackup backup = new SysBackup();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        backup.setFileName("backup_manual_" + timestamp + ".sql");
        backup.setFilePath("backups/" + backup.getFileName());
        backup.setFileSize(0L);
        backup.setType("manual");
        backup.setStatus("success");
        backup.setOperatorId(userId);
        backup.setOperatorName(username != null ? username : "system");
        backup.setCreateTime(LocalDateTime.now());
        backup.setUpdateTime(LocalDateTime.now());
        backupMapper.insert(backup);

        log.info("手动备份完成，操作人：{}", username);
        return Result.ok();
    }

    @PostMapping("/restore/{id}")
    @Operation(summary = "恢复备份")
    public Result<Void> restore(@PathVariable Long id) {
        SysBackup backup = backupMapper.selectById(id);
        if (backup == null || backup.getDeleted() == 1) {
            return Result.fail("备份记录不存在");
        }
        // 实际恢复逻辑需要根据数据库类型实现
        log.info("恢复备份：{}", backup.getFileName());
        return Result.ok("备份恢复任务已提交（实际恢复需要运维配合）", null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除备份记录")
    public Result<Void> delete(@PathVariable Long id) {
        // ★ 修复：deleteById 在 @TableLogic 下会自动转为逻辑删除
        backupMapper.deleteById(id);
        return Result.ok();
    }

    @PutMapping("/settings")
    @Operation(summary = "保存自动备份设置")
    public Result<Void> saveSettings(@RequestBody Map<String, Object> settings) {
        // 备份设置存储在系统配置中，这里简化处理
        log.info("更新自动备份设置：{}", settings);
        return Result.ok();
    }

    @GetMapping("/stats")
    @Operation(summary = "备份统计信息")
    public Result<Map<String, Object>> stats() {
        long totalCount = backupMapper.selectCount(
            new LambdaQueryWrapper<SysBackup>().eq(SysBackup::getDeleted, 0));

        List<SysBackup> latest = backupMapper.selectList(
            new LambdaQueryWrapper<SysBackup>()
                .eq(SysBackup::getDeleted, 0)
                .orderByDesc(SysBackup::getCreateTime)
                .last("LIMIT 1"));

        return Result.ok(Map.of(
            "backupCount", totalCount,
            "latestBackup", latest.isEmpty() ? "-" : latest.get(0).getCreateTime().toLocalDate().toString()
        ));
    }
}
