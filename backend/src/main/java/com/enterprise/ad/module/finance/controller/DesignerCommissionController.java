package com.enterprise.ad.module.finance.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.ad.common.PageResult;
import com.enterprise.ad.common.Result;
import com.enterprise.ad.module.designer.entity.DesignerCommissionConfig;
import com.enterprise.ad.module.designer.mapper.DesignerCommissionConfigMapper;
import com.enterprise.ad.module.finance.entity.DesignerCommission;
import com.enterprise.ad.module.finance.entity.FinanceRecord;
import com.enterprise.ad.module.finance.mapper.DesignerCommissionMapper;
import com.enterprise.ad.module.finance.mapper.FinanceRecordMapper;
import com.enterprise.ad.module.order.entity.Order;
import com.enterprise.ad.module.order.mapper.OrderMapper;
import com.enterprise.ad.module.system.user.entity.SysUser;
import com.enterprise.ad.module.system.user.mapper.SysUserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 设计师提成管理
 * 数据来源于"有设计师的订单"，不再手动新增提成记录
 */
@Slf4j
@RestController("financeDesignerCommissionController")
@RequestMapping("/finance/designer-commission")
@RequiredArgsConstructor
@Tag(name = "设计师提成")
public class DesignerCommissionController {

    private final OrderMapper orderMapper;
    private final DesignerCommissionMapper commissionMapper;
    private final DesignerCommissionConfigMapper configMapper;
    private final FinanceRecordMapper financeRecordMapper;
    private final SysUserMapper userMapper;

    /**
     * 查询所有设计师用户（供前端下拉框）
     */
    @GetMapping("/designers")
    @Operation(summary = "设计师列表（前端下拉框）")
    @PreAuthorize("hasAuthority('finance:view')")
    public Result<List<Map<String, Object>>> designers() {
        List<SysUser> users = userMapper.selectDesignerUsers();
        List<Map<String, Object>> list = users.stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("realName", u.getRealName() != null ? u.getRealName() : u.getUsername());
            return m;
        }).collect(Collectors.toList());
        return Result.ok(list);
    }

    /**
     * 提成列表：查询所有有设计师的订单，合并提成记录状态
     */
    @GetMapping
    @Operation(summary = "提成列表（分页+筛选，数据来源于订单）")
    @PreAuthorize("hasAuthority('finance:view')")
    public Result<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long designerId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {

        // 1. 分页查询有设计师的订单
        Page<Order> orderPage = new Page<>(current, size);
        LambdaQueryWrapper<Order> orderWrapper = new LambdaQueryWrapper<Order>()
                .isNotNull(Order::getDesignerId)
                .eq(designerId != null, Order::getDesignerId, designerId)
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(Order::getDesignerName, keyword)
                        .or().like(Order::getOrderNo, keyword))
                .orderByDesc(Order::getUpdateTime);
        orderMapper.selectPage(orderPage, orderWrapper);

        // 2. 查询这些订单对应的提成记录
        List<Long> orderIds = orderPage.getRecords().stream()
                .map(Order::getId).collect(Collectors.toList());
        List<DesignerCommission> commissionRecords = new ArrayList<>();
        if (!orderIds.isEmpty()) {
            commissionRecords = commissionMapper.selectList(
                    new LambdaQueryWrapper<DesignerCommission>()
                            .in(DesignerCommission::getOrderId, orderIds));
        }
        Map<Long, DesignerCommission> dcMap = commissionRecords.stream()
                .collect(Collectors.toMap(DesignerCommission::getOrderId, c -> c, (a, b) -> a));

        // 3. 合并数据
        List<Map<String, Object>> records = new ArrayList<>();
        for (Order order : orderPage.getRecords()) {
            DesignerCommission dc = dcMap.get(order.getId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", dc != null ? dc.getId() : null);
            row.put("orderId", order.getId());
            row.put("orderNo", order.getOrderNo());
            row.put("designerId", order.getDesignerId());
            row.put("designerName", order.getDesignerName());
            row.put("baseAmount", order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO);
            row.put("commissionRate", dc != null ? dc.getCommissionRate() : BigDecimal.ZERO);
            row.put("commissionAmount", dc != null ? dc.getCommissionAmount() : BigDecimal.ZERO);
            row.put("status", dc != null ? dc.getStatus() : 1); // 无记录显示"待结算"
            row.put("settleTime", dc != null ? dc.getSettleTime() : null);
            row.put("remark", dc != null ? dc.getRemark() : "");
            row.put("createTime", order.getCreateTime() != null
                    ? order.getCreateTime().toString().replace("T", " ") : "");
            records.add(row);
        }

        // 4. 如果有 status 筛选，在内存中过滤
        if (status != null) {
            records = records.stream()
                    .filter(r -> status.equals(r.get("status")))
                    .collect(Collectors.toList());
        }

        return Result.ok(PageResult.of(orderPage.getTotal(), orderPage.getCurrent(), orderPage.getSize(), records));
    }

    /**
     * 提成汇总：按设计师分组统计订单数和金额
     */
    @GetMapping("/summary")
    @Operation(summary = "提成汇总（按设计师分组，数据来源于订单）")
    @PreAuthorize("hasAuthority('finance:view')")
    public Result<List<Map<String, Object>>> summary(
            @RequestParam(required = false) Integer status) {
        // 查询所有有设计师的订单
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .isNotNull(Order::getDesignerId)
                        .orderByDesc(Order::getUpdateTime));

        // 查询所有提成记录
        List<DesignerCommission> allDc = commissionMapper.selectList(
                new LambdaQueryWrapper<DesignerCommission>());
        Map<Long, DesignerCommission> dcMap = allDc.stream()
                .collect(Collectors.toMap(DesignerCommission::getOrderId, c -> c, (a, b) -> a));

        // 按设计师分组
        Map<Long, List<Order>> grouped = orders.stream()
                .collect(Collectors.groupingBy(Order::getDesignerId));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, List<Order>> entry : grouped.entrySet()) {
            List<Order> orderList = entry.getValue();
            String designerName = orderList.get(0).getDesignerName();
            BigDecimal totalBase = orderList.stream()
                    .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalCommission = BigDecimal.ZERO;
            long pending = 0, settled = 0, paid = 0;
            for (Order o : orderList) {
                DesignerCommission dc = dcMap.get(o.getId());
                if (dc != null) {
                    totalCommission = totalCommission.add(dc.getCommissionAmount() != null ? dc.getCommissionAmount() : BigDecimal.ZERO);
                    if (status == null || status == dc.getStatus()) {
                        switch (dc.getStatus()) {
                            case 1: pending++; break;
                            case 2: settled++; break;
                            case 3: paid++; break;
                        }
                    }
                } else {
                    if (status == null || status == 1) pending++;
                }
            }

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("designerId", entry.getKey());
            m.put("designerName", designerName);
            m.put("totalBase", totalBase);
            m.put("totalCommission", totalCommission);
            m.put("count", orderList.size());
            m.put("pendingCount", pending);
            m.put("settledCount", settled);
            m.put("paidCount", paid);
            result.add(m);
        }

        result.sort((a, b) -> ((BigDecimal) b.get("totalCommission")).compareTo((BigDecimal) a.get("totalCommission")));
        return Result.ok(result);
    }

    /**
     * 提成总览
     */
    @GetMapping("/overview")
    @Operation(summary = "提成总览（数据来源于有设计师的订单）")
    @PreAuthorize("hasAuthority('finance:view')")
    public Result<Map<String, Object>> overview() {
        // 所有有设计师的订单
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .isNotNull(Order::getDesignerId));
        long totalCount = orders.size();
        BigDecimal totalBase = orders.stream()
                .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 所有提成记录
        List<DesignerCommission> allDc = commissionMapper.selectList(
                new LambdaQueryWrapper<DesignerCommission>());
        Map<Long, DesignerCommission> dcMap = allDc.stream()
                .collect(Collectors.toMap(DesignerCommission::getOrderId, c -> c, (a, b) -> a));

        BigDecimal pendingAmount = BigDecimal.ZERO;
        BigDecimal settledAmount = BigDecimal.ZERO;
        BigDecimal paidAmount = BigDecimal.ZERO;
        long pendingCount = 0, settledCount = 0, paidCount = 0;

        for (Order o : orders) {
            DesignerCommission dc = dcMap.get(o.getId());
            if (dc != null) {
                BigDecimal amt = dc.getCommissionAmount() != null ? dc.getCommissionAmount() : BigDecimal.ZERO;
                switch (dc.getStatus()) {
                    case 1: pendingAmount = pendingAmount.add(amt); pendingCount++; break;
                    case 2: settledAmount = settledAmount.add(amt); settledCount++; break;
                    case 3: paidAmount = paidAmount.add(amt); paidCount++; break;
                }
            } else {
                pendingCount++; // 没有提成记录的视为待结算
            }
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("pendingAmount", pendingAmount);
        map.put("pendingCount", pendingCount);
        map.put("settledAmount", settledAmount);
        map.put("settledCount", settledCount);
        map.put("paidAmount", paidAmount);
        map.put("paidCount", paidCount);
        map.put("totalAmount", pendingAmount.add(settledAmount).add(paidAmount));
        map.put("totalCount", totalCount);
        return Result.ok(map);
    }

    /**
     * 批量重算提成（按设计师或全部）
     * 提成 = 订单总额 × 设计师提成比例 / 100
     */
    @PostMapping("/recalc")
    @Operation(summary = "批量重算提成（指定设计师或全部）")
    @PreAuthorize("hasAuthority('finance:view')")
    @Transactional
    public Result<Map<String, Object>> recalc(@RequestParam(required = false) Long designerId) {
        // 查询所有需要计算的订单
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .isNotNull(Order::getDesignerId);
        if (designerId != null) {
            wrapper.eq(Order::getDesignerId, designerId);
        }
        List<Order> orders = orderMapper.selectList(wrapper);

        // 查询所有提成配置（按 designerId 索引）
        List<DesignerCommissionConfig> configs = configMapper.selectList(
                new LambdaQueryWrapper<DesignerCommissionConfig>()
                        .eq(DesignerCommissionConfig::getEnabled, 1)
                        .eq(DesignerCommissionConfig::getDeleted, 0));
        Map<Long, BigDecimal> rateMap = configs.stream()
                .collect(Collectors.toMap(
                        DesignerCommissionConfig::getDesignerId,
                        c -> c.getCommissionRate() != null ? c.getCommissionRate() : BigDecimal.ZERO,
                        (a, b) -> a));

        // 查询已有提成记录
        List<Long> orderIds = orders.stream().map(Order::getId).collect(Collectors.toList());
        List<DesignerCommission> existingDcList = orderIds.isEmpty() ? new ArrayList<>() :
                commissionMapper.selectList(
                        new LambdaQueryWrapper<DesignerCommission>()
                                .in(DesignerCommission::getOrderId, orderIds));
        Map<Long, DesignerCommission> existingMap = existingDcList.stream()
                .collect(Collectors.toMap(DesignerCommission::getOrderId, c -> c, (a, b) -> a));

        int updated = 0, created = 0, skipped = 0;
        LocalDateTime now = LocalDateTime.now();

        for (Order order : orders) {
            BigDecimal rate = rateMap.getOrDefault(order.getDesignerId(), BigDecimal.ZERO);
            BigDecimal totalAmount = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal commissionAmount = totalAmount.multiply(rate)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            // 同时更新 ord_order 的设计师提数字段
            Order orderUpdate = new Order();
            orderUpdate.setId(order.getId());
            orderUpdate.setDesignerCommission(commissionAmount);
            orderUpdate.setDesignerName(order.getDesignerName());
            orderUpdate.setUpdateTime(now);
            orderMapper.updateById(orderUpdate);

            DesignerCommission existing = existingMap.get(order.getId());
            if (existing != null) {
                // 更新已有提成记录（只更新金额，不影响状态）
                existing.setBaseAmount(totalAmount);
                existing.setCommissionRate(rate);
                existing.setCommissionAmount(commissionAmount);
                existing.setUpdateTime(now);
                commissionMapper.updateById(existing);
                updated++;
            } else if (rate.compareTo(BigDecimal.ZERO) > 0 || true) { // 即使为0也创建记录
                // 新建提成记录
                SysUser designer = userMapper.selectById(order.getDesignerId());
                String designerName = designer != null && designer.getRealName() != null
                        ? designer.getRealName() : (designer != null ? designer.getUsername() : "");

                DesignerCommission dc = new DesignerCommission();
                dc.setOrderId(order.getId());
                dc.setOrderNo(order.getOrderNo());
                dc.setDesignerId(order.getDesignerId());
                dc.setDesignerName(designerName);
                dc.setBaseAmount(totalAmount);
                dc.setCommissionRate(rate);
                dc.setCommissionAmount(commissionAmount);
                dc.setStatus(1);
                dc.setCreateTime(now);
                dc.setUpdateTime(now);
                commissionMapper.insert(dc);
                created++;
            } else {
                skipped++;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", orders.size());
        result.put("updated", updated);
        result.put("created", created);
        result.put("skipped", skipped);
        log.info("提成批量重算完成: total={}, updated={}, created={}, skipped={}",
                orders.size(), updated, created, skipped);
        return Result.ok(result);
    }

    /**
     * 更新提成记录（编辑提成金额/备注/比例）
     */
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

    /**
     * 更新提成状态（结算/打款）
     * 结算(status 1→2)：写入财务支出流水
     * 打款(status 2→3)：已结算时已记录支出，打款仅标记状态
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "更新提成状态（结算/打款）")
    @PreAuthorize("hasAuthority('finance:view')")
    @Transactional
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        DesignerCommission existing = commissionMapper.selectById(id);
        if (existing == null || existing.getDeleted() == 1) {
            return Result.fail("提成记录不存在");
        }

        // 结算(status 1→2)：产生财务支出
        if (existing.getStatus() == 1 && status == 2
                && existing.getCommissionAmount() != null
                && existing.getCommissionAmount().compareTo(BigDecimal.ZERO) > 0) {
            FinanceRecord finRecord = new FinanceRecord();
            finRecord.setRecordNo("DC" + System.currentTimeMillis());
            finRecord.setType("expense");
            finRecord.setCategory("设计师提成");
            finRecord.setAmount(existing.getCommissionAmount());
            finRecord.setRelatedId(existing.getDesignerId());
            finRecord.setRelatedName(existing.getDesignerName());
            finRecord.setRemark("提成结算 | 订单: " + existing.getOrderNo());
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

    /**
     * 批量结算：将某设计师所有待结算(status=1)的提成改为已结算(status=2)
     * 同时写入财务支出流水
     */
    @PostMapping("/batch-settle")
    @Operation(summary = "批量结算（按设计师，所有待结算→已结算，写入财务支出）")
    @PreAuthorize("hasAuthority('finance:view')")
    @Transactional
    public Result<Map<String, Object>> batchSettle(@RequestParam Long designerId) {
        List<DesignerCommission> pendingList = commissionMapper.selectList(
                new LambdaQueryWrapper<DesignerCommission>()
                        .eq(DesignerCommission::getDesignerId, designerId)
                        .eq(DesignerCommission::getStatus, 1));

        int count = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        LocalDateTime now = LocalDateTime.now();

        for (DesignerCommission dc : pendingList) {
            // 写入财务支出
            if (dc.getCommissionAmount() != null
                    && dc.getCommissionAmount().compareTo(BigDecimal.ZERO) > 0) {
                FinanceRecord finRecord = new FinanceRecord();
                finRecord.setRecordNo("DC" + System.currentTimeMillis() + count);
                finRecord.setType("expense");
                finRecord.setCategory("设计师提成");
                finRecord.setAmount(dc.getCommissionAmount());
                finRecord.setRelatedId(dc.getDesignerId());
                finRecord.setRelatedName(dc.getDesignerName());
                finRecord.setRemark("提成批量结算 | 订单: " + dc.getOrderNo());
                finRecord.setCreateTime(now);
                finRecord.setDeleted(0);
                financeRecordMapper.insert(finRecord);
                totalAmount = totalAmount.add(dc.getCommissionAmount());
            }

            dc.setStatus(2);
            dc.setSettleTime(now);
            dc.setUpdateTime(now);
            commissionMapper.updateById(dc);
            count++;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", count);
        result.put("totalAmount", totalAmount);
        log.info("批量结算完成: designerId={}, count={}, totalAmount={}", designerId, count, totalAmount);
        return Result.ok(result);
    }

    /**
     * 批量打款：将某设计师所有已结算(status=2)的提成改为已打款(status=3)
     */
    @PostMapping("/batch-pay")
    @Operation(summary = "批量打款（按设计师，所有已结算→已打款）")
    @PreAuthorize("hasAuthority('finance:view')")
    @Transactional
    public Result<Map<String, Object>> batchPay(@RequestParam Long designerId) {
        List<DesignerCommission> settledList = commissionMapper.selectList(
                new LambdaQueryWrapper<DesignerCommission>()
                        .eq(DesignerCommission::getDesignerId, designerId)
                        .eq(DesignerCommission::getStatus, 2));

        int count = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        LocalDateTime now = LocalDateTime.now();

        for (DesignerCommission dc : settledList) {
            totalAmount = totalAmount.add(
                    dc.getCommissionAmount() != null ? dc.getCommissionAmount() : BigDecimal.ZERO);
            dc.setStatus(3);
            dc.setUpdateTime(now);
            commissionMapper.updateById(dc);
            count++;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", count);
        result.put("totalAmount", totalAmount);
        log.info("批量打款完成: designerId={}, count={}, totalAmount={}", designerId, count, totalAmount);
        return Result.ok(result);
    }

    /**
     * 删除提成记录
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除提成记录")
    @PreAuthorize("hasAuthority('finance:view')")
    public Result<Void> delete(@PathVariable Long id) {
        commissionMapper.deleteById(id);
        return Result.ok();
    }

    /**
     * 设置设计师提成比例，并自动重算该设计师所有订单
     * 这是设计师配置页面调用的快捷入口
     */
    @PostMapping("/set-rate")
    @Operation(summary = "设置提成比例并自动重算该设计师所有订单")
    @PreAuthorize("hasAuthority('finance:view')")
    @Transactional
    public Result<Map<String, Object>> setRateAndRecalc(
            @RequestParam Long designerId,
            @RequestParam BigDecimal rate,
            @RequestParam(defaultValue = "true") boolean enabled) {
        // 保存或更新提成配置
        DesignerCommissionConfig config = configMapper.selectOne(
                new LambdaQueryWrapper<DesignerCommissionConfig>()
                        .eq(DesignerCommissionConfig::getDesignerId, designerId)
                        .eq(DesignerCommissionConfig::getDeleted, 0));

        SysUser designer = userMapper.selectById(designerId);
        String designerName = designer != null && designer.getRealName() != null
                ? designer.getRealName() : (designer != null ? designer.getUsername() : "");

        if (config != null) {
            config.setCommissionRate(rate);
            config.setEnabled(enabled ? 1 : 0);
            config.setDesignerName(designerName);
            config.setUpdatedTime(LocalDateTime.now());
            configMapper.updateById(config);
        } else {
            config = new DesignerCommissionConfig();
            config.setDesignerId(designerId);
            config.setDesignerName(designerName);
            config.setCommissionRate(rate);
            config.setEnabled(enabled ? 1 : 0);
            config.setCreateTime(LocalDateTime.now());
            config.setDeleted(0);
            configMapper.insert(config);
        }

        // 自动重算该设计师所有订单
        return recalc(designerId);
    }
}
