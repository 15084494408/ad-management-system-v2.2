package com.enterprise.ad.module.finance.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.ad.common.PageResult;
import com.enterprise.ad.common.Result;
import com.enterprise.ad.module.finance.entity.DesignerCommission;
import com.enterprise.ad.module.finance.mapper.DesignerCommissionMapper;
import com.enterprise.ad.module.finance.entity.FinanceRecord;
import com.enterprise.ad.module.finance.mapper.FinanceRecordMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 设计师提成管理（财务视角：查看订单提成记录）
 */
@RestController("financeDesignerCommissionController")
@RequestMapping("/finance/designer-commission")
@RequiredArgsConstructor
@Tag(name = "设计师提成")
public class DesignerCommissionController {

    private final DesignerCommissionMapper commissionMapper;
    private final FinanceRecordMapper financeRecordMapper;

    @GetMapping
    @Operation(summary = "提成列表（分页+筛选）")
    @PreAuthorize("hasAuthority('finance:view')")
    public Result<PageResult<DesignerCommission>> list(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long designerId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        Page<DesignerCommission> page = new Page<>(current, size);
        LambdaQueryWrapper<DesignerCommission> wrapper = new LambdaQueryWrapper<DesignerCommission>()
                .eq(designerId != null, DesignerCommission::getDesignerId, designerId)
                .eq(status != null, DesignerCommission::getStatus, status)
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(DesignerCommission::getDesignerName, keyword)
                        .or().like(DesignerCommission::getOrderNo, keyword)
                        .or().like(DesignerCommission::getRemark, keyword))
                .orderByDesc(DesignerCommission::getCreateTime);
        commissionMapper.selectPage(page, wrapper);
        return Result.ok(PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    @GetMapping("/summary")
    @Operation(summary = "提成汇总（按设计师分组）")
    @PreAuthorize("hasAuthority('finance:view')")
    public Result<List<Map<String, Object>>> summary(
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<DesignerCommission> wrapper = new LambdaQueryWrapper<DesignerCommission>()
                .eq(status != null, DesignerCommission::getStatus, status);
        List<DesignerCommission> all = commissionMapper.selectList(wrapper);

        // 按设计师分组汇总
        Map<Long, List<DesignerCommission>> grouped = all.stream()
                .collect(Collectors.groupingBy(DesignerCommission::getDesignerId));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, List<DesignerCommission>> entry : grouped.entrySet()) {
            List<DesignerCommission> items = entry.getValue();
            String name = items.get(0).getDesignerName();
            BigDecimal totalBase = items.stream().map(DesignerCommission::getBaseAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalCommission = items.stream().map(DesignerCommission::getCommissionAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            long pending = items.stream().filter(c -> c.getStatus() == 1).count();
            long settled = items.stream().filter(c -> c.getStatus() == 2).count();
            long paid = items.stream().filter(c -> c.getStatus() == 3).count();

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("designerId", entry.getKey());
            m.put("designerName", name);
            m.put("totalBase", totalBase);
            m.put("totalCommission", totalCommission);
            m.put("count", items.size());
            m.put("pendingCount", pending);
            m.put("settledCount", settled);
            m.put("paidCount", paid);
            result.add(m);
        }
        // 按总提成金额降序
        result.sort((a, b) -> ((BigDecimal) b.get("totalCommission")).compareTo((BigDecimal) a.get("totalCommission")));
        return Result.ok(result);
    }

    @PostMapping
    @Operation(summary = "新建提成记录")
    @PreAuthorize("hasAuthority('finance:view')")
    public Result<Long> create(@RequestBody DesignerCommission commission) {
        commission.setCreateTime(LocalDateTime.now());
        commission.setUpdateTime(LocalDateTime.now());
        if (commission.getStatus() == null) commission.setStatus(1);
        if (commission.getBaseAmount() == null) commission.setBaseAmount(BigDecimal.ZERO);
        if (commission.getCommissionRate() == null) commission.setCommissionRate(BigDecimal.ZERO);
        if (commission.getCommissionAmount() == null) commission.setCommissionAmount(BigDecimal.ZERO);
        // 自动计算提成金额 = 基数 * 比例 / 100
        if (commission.getCommissionAmount().compareTo(BigDecimal.ZERO) == 0
                && commission.getBaseAmount().compareTo(BigDecimal.ZERO) > 0
                && commission.getCommissionRate().compareTo(BigDecimal.ZERO) > 0) {
            commission.setCommissionAmount(
                    commission.getBaseAmount()
                            .multiply(commission.getCommissionRate())
                            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
        }
        commissionMapper.insert(commission);
        return Result.ok(commission.getId());
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新提成记录")
    @PreAuthorize("hasAuthority('finance:view')")
    public Result<Void> update(@PathVariable Long id, @RequestBody DesignerCommission commission) {
        DesignerCommission existing = commissionMapper.selectById(id);
        if (existing == null || existing.getDeleted() == 1) {
            return Result.fail("提成记录不存在");
        }
        commission.setId(id);
        commission.setUpdateTime(LocalDateTime.now());
        // 重新计算提成金额
        if (commission.getBaseAmount() != null && commission.getCommissionRate() != null
                && commission.getBaseAmount().compareTo(BigDecimal.ZERO) > 0
                && commission.getCommissionRate().compareTo(BigDecimal.ZERO) > 0) {
            commission.setCommissionAmount(
                    commission.getBaseAmount()
                            .multiply(commission.getCommissionRate())
                            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
        }
        commissionMapper.updateById(commission);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "更新提成状态（结算/打款）")
    @PreAuthorize("hasAuthority('finance:view')")
    @org.springframework.transaction.annotation.Transactional
    public Result<Void> updateStatus(
            @PathVariable Long id,
            @RequestParam Integer status) {
        DesignerCommission existing = commissionMapper.selectById(id);
        if (existing == null || existing.getDeleted() == 1) {
            return Result.fail("提成记录不存在");
        }

        // 从非打款状态 → 已打款(status=3) 时，生成财务支出流水
        if (existing.getStatus() < 3 && status == 3
                && existing.getCommissionAmount() != null
                && existing.getCommissionAmount().compareTo(BigDecimal.ZERO) > 0) {
            FinanceRecord finRecord = new FinanceRecord();
            finRecord.setRecordNo("DC" + System.currentTimeMillis());
            finRecord.setType("expense");
            finRecord.setCategory("设计师提成");
            finRecord.setAmount(existing.getCommissionAmount());
            finRecord.setRelatedId(existing.getDesignerId());
            finRecord.setRelatedName(existing.getDesignerName());
            finRecord.setRemark("提成打款 | 订单: " + existing.getOrderNo());
            finRecord.setCreateTime(LocalDateTime.now());
            finRecord.setDeleted(0);
            financeRecordMapper.insert(finRecord);
        }

        existing.setStatus(status);
        existing.setUpdateTime(LocalDateTime.now());
        if (status >= 2) {
            existing.setSettleTime(LocalDateTime.now());
        }
        commissionMapper.updateById(existing);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除提成记录")
    @PreAuthorize("hasAuthority('finance:view')")
    public Result<Void> delete(@PathVariable Long id) {
        commissionMapper.deleteById(id);
        return Result.ok();
    }

    @GetMapping("/overview")
    @Operation(summary = "提成总览（总待结算/已结算/已打款金额）")
    @PreAuthorize("hasAuthority('finance:view')")
    public Result<Map<String, Object>> overview() {
        List<DesignerCommission> all = commissionMapper.selectList(
                new LambdaQueryWrapper<DesignerCommission>());

        BigDecimal pendingAmount = BigDecimal.ZERO;
        BigDecimal settledAmount = BigDecimal.ZERO;
        BigDecimal paidAmount = BigDecimal.ZERO;
        for (DesignerCommission c : all) {
            switch (c.getStatus()) {
                case 1: pendingAmount = pendingAmount.add(c.getCommissionAmount()); break;
                case 2: settledAmount = settledAmount.add(c.getCommissionAmount()); break;
                case 3: paidAmount = paidAmount.add(c.getCommissionAmount()); break;
            }
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("pendingAmount", pendingAmount);
        map.put("pendingCount", all.stream().filter(c -> c.getStatus() == 1).count());
        map.put("settledAmount", settledAmount);
        map.put("settledCount", all.stream().filter(c -> c.getStatus() == 2).count());
        map.put("paidAmount", paidAmount);
        map.put("paidCount", all.stream().filter(c -> c.getStatus() == 3).count());
        map.put("totalAmount", pendingAmount.add(settledAmount).add(paidAmount));
        map.put("totalCount", all.size());
        return Result.ok(map);
    }
}
