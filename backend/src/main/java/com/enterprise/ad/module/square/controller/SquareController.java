package com.enterprise.ad.module.square.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.ad.common.PageResult;
import com.enterprise.ad.common.Result;
import com.enterprise.ad.common.util.WebUtil;
import com.enterprise.ad.module.square.entity.*;
import com.enterprise.ad.module.square.mapper.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/square")
@RequiredArgsConstructor
@Tag(name = "设计广场")
public class SquareController {

    private final SquareRequirementMapper requirementMapper;
    private final SquareApplicationMapper applicationMapper;
    private final SquareIncomeMapper incomeMapper;

    // ========== 需求广场 ==========

    @GetMapping("/requirement")
    @Operation(summary = "需求列表（分页）")
    public Result<PageResult<SquareRequirement>> listRequirements(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long publisherId) {

        Page<SquareRequirement> page = new Page<>(current, size);
        LambdaQueryWrapper<SquareRequirement> qw = new LambdaQueryWrapper<SquareRequirement>()
            .eq(SquareRequirement::getDeleted, 0)
            .eq(category != null, SquareRequirement::getCategory, category)
            .eq(status != null, SquareRequirement::getStatus, status)
            .eq(publisherId != null, SquareRequirement::getPublisherId, publisherId)
            .orderByDesc(SquareRequirement::getCreateTime);

        Page<SquareRequirement> result = requirementMapper.selectPage(page, qw);
        return Result.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @GetMapping("/requirement/{id}")
    @Operation(summary = "需求详情")
    public Result<SquareRequirement> getRequirement(@PathVariable Long id) {
        SquareRequirement req = requirementMapper.selectById(id);
        if (req != null) {
            req.setViewCount(req.getViewCount() + 1);
            requirementMapper.updateById(req);
        }
        return Result.ok(req);
    }

    @PostMapping("/requirement")
    @Operation(summary = "发布需求")
    public Result<Void> createRequirement(@RequestBody SquareRequirement req, HttpServletRequest request) {
        // ★ 修复：从 request attribute 获取用户信息
        Long userId = WebUtil.getCurrentUserId(request);
        String username = WebUtil.getCurrentUsername(request);

        req.setPublisherId(userId);
        req.setPublisherName(username != null ? username : "unknown");
        req.setStatus(1);
        req.setViewCount(0);
        req.setApplyCount(0);
        req.setCreateTime(LocalDateTime.now());
        requirementMapper.insert(req);
        return Result.ok();
    }

    @PutMapping("/requirement/{id}")
    @Operation(summary = "更新需求")
    public Result<Void> updateRequirement(@PathVariable Long id, @RequestBody SquareRequirement req) {
        req.setId(id);
        req.setUpdateTime(LocalDateTime.now());
        requirementMapper.updateById(req);
        return Result.ok();
    }

    @DeleteMapping("/requirement/{id}")
    @Operation(summary = "删除需求")
    public Result<Void> deleteRequirement(@PathVariable Long id) {
        // ★ 修复：deleteById 在 @TableLogic 下会自动转为逻辑删除
        requirementMapper.deleteById(id);
        return Result.ok();
    }

    // ========== 我的接单 ==========

    @GetMapping("/my-apply")
    @Operation(summary = "我的申请列表")
    public Result<PageResult<SquareApplication>> myApplications(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            HttpServletRequest request) {

        // ★ 修复：从 request attribute 获取用户信息
        Long userId = (Long) request.getAttribute("userId");

        Page<SquareApplication> page = new Page<>(current, size);
        LambdaQueryWrapper<SquareApplication> qw = new LambdaQueryWrapper<SquareApplication>()
            .eq(SquareApplication::getDeleted, 0)
            .eq(SquareApplication::getDesignerId, userId)
            .orderByDesc(SquareApplication::getCreateTime);

        Page<SquareApplication> result = applicationMapper.selectPage(page, qw);
        return Result.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @GetMapping("/requirement/{id}/applications")
    @Operation(summary = "需求申请列表")
    public Result<?> getApplications(@PathVariable Long id) {
        return Result.ok(applicationMapper.selectList(
            new LambdaQueryWrapper<SquareApplication>()
                .eq(SquareApplication::getRequirementId, id)
                .eq(SquareApplication::getDeleted, 0)
                .orderByDesc(SquareApplication::getCreateTime)
        ));
    }

    @PostMapping("/apply")
    @Operation(summary = "申请接单")
    @Transactional
    public Result<Void> apply(@RequestBody SquareApplication app, HttpServletRequest request) {
        Long userId = WebUtil.getCurrentUserId(request);
        String username = WebUtil.getCurrentUsername(request);

        // 检查是否已申请
        long count = applicationMapper.selectCount(
            new LambdaQueryWrapper<SquareApplication>()
                .eq(SquareApplication::getRequirementId, app.getRequirementId())
                .eq(SquareApplication::getDesignerId, userId)
                .eq(SquareApplication::getDeleted, 0)
        );
        if (count > 0) {
            return Result.fail(400, "您已申请过该需求");
        }

        app.setDesignerId(userId);
        app.setDesignerName(username != null ? username : "unknown");
        app.setStatus(1);
        app.setCreateTime(LocalDateTime.now());
        applicationMapper.insert(app);

        // 更新需求申请数
        SquareRequirement req = requirementMapper.selectById(app.getRequirementId());
        if (req != null) {
            req.setApplyCount(req.getApplyCount() + 1);
            requirementMapper.updateById(req);
        }
        return Result.ok();
    }

    @PutMapping("/apply/{id}/accept")
    @Operation(summary = "接受申请")
    @PreAuthorize("hasAuthority('square:manage')")
    @Transactional
    public Result<Void> acceptApply(@PathVariable Long id) {
        SquareApplication app = applicationMapper.selectById(id);
        if (app != null) {
            app.setStatus(2);
            app.setUpdateTime(LocalDateTime.now());
            applicationMapper.updateById(app);

            // 更新需求状态
            SquareRequirement req = requirementMapper.selectById(app.getRequirementId());
            if (req != null) {
                req.setStatus(2);
                req.setDesignerId(app.getDesignerId());
                req.setDesignerName(app.getDesignerName());
                req.setUpdateTime(LocalDateTime.now());
                requirementMapper.updateById(req);
            }
        }
        return Result.ok();
    }

    @PutMapping("/apply/{id}/reject")
    @Operation(summary = "拒绝申请")
    @PreAuthorize("hasAuthority('square:manage')")
    public Result<Void> rejectApply(@PathVariable Long id, @RequestParam String reason) {
        SquareApplication app = new SquareApplication();
        app.setId(id);
        app.setStatus(3);
        app.setRejectReason(reason);
        app.setUpdateTime(LocalDateTime.now());
        applicationMapper.updateById(app);
        return Result.ok();
    }

    // ========== 收入管理 ==========

    @GetMapping("/income")
    @Operation(summary = "收入列表")
    public Result<PageResult<SquareIncome>> listIncome(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");

        Page<SquareIncome> page = new Page<>(current, size);
        LambdaQueryWrapper<SquareIncome> qw = new LambdaQueryWrapper<SquareIncome>()
            .eq(SquareIncome::getDeleted, 0)
            .eq(SquareIncome::getDesignerId, userId)
            .orderByDesc(SquareIncome::getCreateTime);

        Page<SquareIncome> result = incomeMapper.selectPage(page, qw);
        return Result.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @GetMapping("/income/summary")
    @Operation(summary = "收入汇总")
    public Result<?> incomeSummary(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        var qw = new LambdaQueryWrapper<SquareIncome>()
            .eq(SquareIncome::getDeleted, 0)
            .eq(SquareIncome::getDesignerId, userId);

        var list = incomeMapper.selectList(qw);
        BigDecimal totalIncome = list.stream().map(SquareIncome::getActualIncome).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalQuoted = list.stream().map(SquareIncome::getQuotedPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        long completed = list.stream().filter(i -> i.getStatus() >= 2).count();

        return Result.ok(java.util.Map.of(
            "totalIncome", totalIncome,
            "totalQuoted", totalQuoted,
            "completedCount", completed,
            "pendingCount", list.size() - completed
        ));
    }

    @GetMapping("/income/export")
    @Operation(summary = "导出收入统计")
    public void exportIncome(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_OK);
    }
}
