package com.enterprise.ad.module.statistics.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.ad.common.Result;
import com.enterprise.ad.module.customer.mapper.CustomerMapper;
import com.enterprise.ad.module.customer.entity.Customer;
import com.enterprise.ad.module.finance.mapper.FinanceRecordMapper;
import com.enterprise.ad.module.finance.entity.FinanceRecord;
import com.enterprise.ad.module.material.mapper.MaterialMapper;
import com.enterprise.ad.module.material.entity.Material;
import com.enterprise.ad.module.order.entity.Order;
import com.enterprise.ad.module.order.mapper.OrderMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
@Tag(name = "统计分析")
@PreAuthorize("hasAuthority('statistics:view')")
public class StatisticsController {

    private final OrderMapper orderMapper;
    private final CustomerMapper customerMapper;
    private final FinanceRecordMapper financeRecordMapper;
    private final MaterialMapper materialMapper;

    // ========== 订单统计 ==========

    @GetMapping("/order/summary")
    @Operation(summary = "订单统计摘要")
    public Result<Map<String, Object>> orderSummary(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        
        LambdaQueryWrapper<Order> qw = new LambdaQueryWrapper<Order>()
            .eq(Order::getDeleted, 0)
            .ne(Order::getStatus, 4);  // ★ 修复：排除已取消订单，避免取消订单金额计入统计
        if (startDate != null) qw.ge(Order::getCreateTime, startDate.atStartOfDay());
        if (endDate != null) qw.le(Order::getCreateTime, endDate.atTime(23, 59, 59));
        
        List<Order> orders = orderMapper.selectList(qw);

        // 单独查询已取消订单数量
        LambdaQueryWrapper<Order> cancelQw = new LambdaQueryWrapper<Order>()
            .eq(Order::getDeleted, 0)
            .eq(Order::getStatus, 4);
        if (startDate != null) cancelQw.ge(Order::getCreateTime, startDate.atStartOfDay());
        if (endDate != null) cancelQw.le(Order::getCreateTime, endDate.atTime(23, 59, 59));
        long cancelledCount = orderMapper.selectCount(cancelQw);

        BigDecimal totalAmount = orders.stream().map(Order::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paidAmount = orders.stream().map(Order::getPaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        long pendingCount = orders.stream().filter(o -> o.getStatus() == 1).count();
        long processingCount = orders.stream().filter(o -> o.getStatus() == 2).count();
        long completedCount = orders.stream().filter(o -> o.getStatus() == 3).count();

        return Result.ok(Map.of(
            "totalOrders", orders.size(),
            "totalAmount", totalAmount,
            "paidAmount", paidAmount,
            "unpaidAmount", totalAmount.subtract(paidAmount).max(BigDecimal.ZERO),
            "pendingCount", pendingCount,
            "processingCount", processingCount,
            "completedCount", completedCount,
            "cancelledCount", cancelledCount
        ));
    }

    @GetMapping("/order/trend")
    @Operation(summary = "订单趋势（日/周/月）")
    public Result<List<Map<String, Object>>> orderTrend(
            @RequestParam(defaultValue = "daily") String type,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        
        if (startDate == null) startDate = LocalDate.now().minusDays(30);
        if (endDate == null) endDate = LocalDate.now();
        
        List<Order> orders = orderMapper.selectList(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getDeleted, 0)
                .ne(Order::getStatus, 4)  // ★ 排除已取消订单
                .ge(Order::getCreateTime, startDate.atStartOfDay())
                .le(Order::getCreateTime, endDate.atTime(23, 59, 59))
        );
        
        Map<String, List<Order>> grouped;
        java.time.LocalDateTime finalEndDate1 = endDate.atTime(23, 59, 59);
        java.time.LocalDate start = startDate;
        java.time.LocalDate end = finalEndDate1.toLocalDate();
        if ("daily".equals(type)) {
            grouped = orders.stream().collect(Collectors.groupingBy(
                o -> o.getCreateTime().toLocalDate().toString()
            ));
        } else if ("weekly".equals(type)) {
            grouped = orders.stream().collect(Collectors.groupingBy(
                o -> o.getCreateTime().toLocalDate().with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).toString()
            ));
        } else {
            grouped = orders.stream().collect(Collectors.groupingBy(
                o -> o.getCreateTime().toLocalDate().withDayOfMonth(1).toString()
            ));
        }
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<Order>> entry : grouped.entrySet()) {
            result.add(Map.of(
                "date", entry.getKey(),
                "count", entry.getValue().size(),
                "amount", entry.getValue().stream().map(Order::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add)
            ));
        }
        result.sort(Comparator.comparing(m -> m.get("date").toString()));
        return Result.ok(result);
    }

    // ========== 营收统计 ==========

    @GetMapping("/revenue/summary")
    @Operation(summary = "营收统计摘要")
    public Result<Map<String, Object>> revenueSummary(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        
        LambdaQueryWrapper<FinanceRecord> qw = new LambdaQueryWrapper<FinanceRecord>()
            .eq(FinanceRecord::getDeleted, 0)
            .eq(FinanceRecord::getType, "income");
        if (startDate != null) qw.ge(FinanceRecord::getCreateTime, startDate.atStartOfDay());
        if (endDate != null) qw.le(FinanceRecord::getCreateTime, endDate.atTime(23, 59, 59));
        
        List<FinanceRecord> records = financeRecordMapper.selectList(qw);
        BigDecimal total = records.stream().map(FinanceRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal wechat = records.stream().filter(r -> "wechat".equals(r.getPaymentMethod())).map(FinanceRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal alipay = records.stream().filter(r -> "alipay".equals(r.getPaymentMethod())).map(FinanceRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cash = records.stream().filter(r -> "cash".equals(r.getPaymentMethod())).map(FinanceRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal transfer = records.stream().filter(r -> "transfer".equals(r.getPaymentMethod())).map(FinanceRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return Result.ok(Map.of(
            "totalRevenue", total,
            "totalCount", records.size(),
            "wechatRevenue", wechat,
            "alipayRevenue", alipay,
            "cashRevenue", cash,
            "transferRevenue", transfer,
            "avgPerOrder", records.isEmpty() ? BigDecimal.ZERO : total.divide(new BigDecimal(records.size()), 2, java.math.RoundingMode.HALF_UP)
        ));
    }

    @GetMapping("/revenue/trend")
    @Operation(summary = "营收趋势")
    public Result<List<Map<String, Object>>> revenueTrend(
            @RequestParam(defaultValue = "daily") String type,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        
        if (startDate == null) startDate = LocalDate.now().minusDays(30);
        if (endDate == null) endDate = LocalDate.now();
        
        List<FinanceRecord> records = financeRecordMapper.selectList(
            new LambdaQueryWrapper<FinanceRecord>()
                .eq(FinanceRecord::getDeleted, 0)
                .eq(FinanceRecord::getType, "income")
                .ge(FinanceRecord::getCreateTime, startDate.atStartOfDay())
                .le(FinanceRecord::getCreateTime, endDate.atTime(23, 59, 59))
        );
        
        Map<String, List<FinanceRecord>> grouped = records.stream().collect(Collectors.groupingBy(
            r -> {
                LocalDate d = r.getCreateTime().toLocalDate();
                if ("daily".equals(type)) return d.toString();
                if ("weekly".equals(type)) return d.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).toString();
                return d.withDayOfMonth(1).toString();
            }
        ));
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<FinanceRecord>> entry : grouped.entrySet()) {
            result.add(Map.of(
                "date", entry.getKey(),
                "amount", entry.getValue().stream().map(FinanceRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add),
                "count", entry.getValue().size()
            ));
        }
        result.sort(Comparator.comparing(m -> m.get("date").toString()));
        return Result.ok(result);
    }

    @Deprecated // [P2-01] 前端未调用此接口
    @GetMapping("/revenue/by-type")
    @Operation(summary = "营收构成（已弃用）")
    public Result<List<Map<String, Object>>> revenueByType(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        
        LambdaQueryWrapper<FinanceRecord> qw = new LambdaQueryWrapper<FinanceRecord>()
            .eq(FinanceRecord::getDeleted, 0)
            .eq(FinanceRecord::getType, "income");
        if (startDate != null) qw.ge(FinanceRecord::getCreateTime, startDate.atStartOfDay());
        if (endDate != null) qw.le(FinanceRecord::getCreateTime, endDate.atTime(23, 59, 59));
        
        List<FinanceRecord> records = financeRecordMapper.selectList(qw);
        
        Map<String, List<FinanceRecord>> grouped = records.stream().collect(Collectors.groupingBy(
            r -> r.getCategory() != null ? r.getCategory() : "其他"
        ));
        
        BigDecimal total = records.stream().map(FinanceRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<FinanceRecord>> entry : grouped.entrySet()) {
            BigDecimal sum = entry.getValue().stream().map(FinanceRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            result.add(Map.of(
                "type", entry.getKey(),
                "amount", sum,
                "count", entry.getValue().size(),
                "percent", total.compareTo(BigDecimal.ZERO) > 0 
                    ? sum.multiply(new BigDecimal("100")).divide(total, 1, java.math.RoundingMode.HALF_UP) 
                    : BigDecimal.ZERO
            ));
        }
        result.sort((a, b) -> new BigDecimal(b.get("amount").toString()).compareTo(new BigDecimal(a.get("amount").toString())));
        return Result.ok(result);
    }

    // ========== 客户统计 ==========

    @GetMapping("/customer/summary")
    @Operation(summary = "客户统计摘要")
    public Result<Map<String, Object>> customerSummary() {
        long total = customerMapper.selectCount(new LambdaQueryWrapper<Customer>().eq(Customer::getDeleted, 0));
        long active = customerMapper.selectCount(
            new LambdaQueryWrapper<Customer>()
                .eq(Customer::getDeleted, 0)
                .ge(Customer::getUpdateTime, LocalDateTime.now().minusMonths(3))
        );
        
        return Result.ok(Map.of(
            "totalCustomers", total,
            "activeCustomers", active,
            "inactiveCustomers", total - active
        ));
    }

    // ========== 物料统计 ==========

    @GetMapping("/material/summary")
    @Operation(summary = "物料统计摘要")
    public Result<Map<String, Object>> materialSummary() {
        long total = materialMapper.selectCount(new LambdaQueryWrapper<Material>().eq(Material::getDeleted, 0));
        long warning = materialMapper.selectCount(
            new LambdaQueryWrapper<Material>()
                .eq(Material::getDeleted, 0)
                .apply("stock_quantity <= warning_quantity")
        );
        
        return Result.ok(Map.of(
            "totalTypes", total,
            "warningTypes", warning,
            "normalTypes", total - warning
        ));
    }

    // ========== 资金流水统计 ==========

    @GetMapping("/flow/summary")
    @Operation(summary = "流水统计汇总（总额/收入/支出/日均/今日统计）")
    public Result<Map<String, Object>> flowSummary(
            @RequestParam(defaultValue = "daily") String period,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        if (startDate == null) startDate = LocalDate.now().minusDays(30);
        if (endDate == null) endDate = LocalDate.now();

        LambdaQueryWrapper<FinanceRecord> qw = new LambdaQueryWrapper<FinanceRecord>()
            .eq(FinanceRecord::getDeleted, 0)
            .ge(FinanceRecord::getCreateTime, startDate.atStartOfDay())
            .le(FinanceRecord::getCreateTime, endDate.atTime(23, 59, 59));

        List<FinanceRecord> records = financeRecordMapper.selectList(qw);

        BigDecimal incomeTotal = records.stream()
            .filter(r -> "income".equals(r.getType()))
            .map(FinanceRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expenseTotal = records.stream()
            .filter(r -> "expense".equals(r.getType()))
            .map(FinanceRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalFlow = incomeTotal.add(expenseTotal.abs());

        // 计算日均流水（按实际天数）
        long days = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
        BigDecimal avgDaily = totalFlow.compareTo(BigDecimal.ZERO) > 0
            ? totalFlow.divide(new BigDecimal(days), 0, java.math.RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        // ── 今日统计 ──
        LocalDate today = LocalDate.now();
        List<FinanceRecord> todayRecords = financeRecordMapper.selectList(
            new LambdaQueryWrapper<FinanceRecord>()
                .eq(FinanceRecord::getDeleted, 0)
                .ge(FinanceRecord::getCreateTime, today.atStartOfDay())
                .le(FinanceRecord::getCreateTime, today.atTime(23, 59, 59))
        );
        BigDecimal todayIncome = todayRecords.stream()
            .filter(r -> "income".equals(r.getType()))
            .map(FinanceRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal todayExpense = todayRecords.stream()
            .filter(r -> "expense".equals(r.getType()))
            .map(FinanceRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal todayTotal = todayIncome.add(todayExpense.abs());
        int todayCount = todayRecords.size();
        BigDecimal avgPerRecord = todayCount > 0
            ? todayTotal.divide(new BigDecimal(todayCount), 2, java.math.RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
        BigDecimal maxRecord = todayRecords.stream()
            .map(FinanceRecord::getAmount)
            .max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

        // 收入渠道占比
        Map<String, BigDecimal> channelMap = new LinkedHashMap<>();
        records.stream().filter(r -> "income".equals(r.getType())).forEach(r -> {
            String ch = r.getPaymentMethod() != null ? r.getPaymentMethod() : "其他";
            channelMap.merge(ch, r.getAmount(), BigDecimal::add);
        });

        BigDecimal incomeFinal = incomeTotal;
        List<Map<String, Object>> channelData = new ArrayList<>();
        String[] colors = {"#409eff", "#67c23a", "#e6a23c", "#f56c6c", "#909399"};
        int idx = 0;
        for (Map.Entry<String, BigDecimal> entry : channelMap.entrySet()) {
            BigDecimal pct = incomeFinal.compareTo(BigDecimal.ZERO) > 0
                ? entry.getValue().multiply(new BigDecimal("100")).divide(incomeFinal, 1, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
            channelData.add(Map.of(
                "label", entry.getKey(),
                "amount", entry.getValue(),
                "pct", pct,
                "color", colors[idx++ % colors.length]
            ));
        }
        if (channelData.isEmpty()) {
            channelData.add(Map.of("label", "暂无数据", "amount", BigDecimal.ZERO, "pct", BigDecimal.ZERO, "color", "#909399"));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalFlow", totalFlow);
        result.put("incomeTotal", incomeTotal);
        result.put("expenseTotal", expenseTotal);
        result.put("avgDaily", avgDaily);
        result.put("channelData", channelData);
        result.put("recordCount", records.size());
        // ── 今日统计字段 ──
        result.put("todayTotal", todayTotal);
        result.put("todayCount", todayCount);
        result.put("avgPerRecord", avgPerRecord);
        result.put("maxRecord", maxRecord);
        result.put("netAmount", incomeTotal.subtract(expenseTotal));
        return Result.ok(result);
    }

    @GetMapping("/flow/trend")
    @Operation(summary = "收支趋势（按日/周/月分组）")
    public Result<List<Map<String, Object>>> flowTrend(
            @RequestParam(defaultValue = "daily") String period,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        if (startDate == null) startDate = LocalDate.now().minusDays(7);
        if (endDate == null) endDate = LocalDate.now();

        List<FinanceRecord> records = financeRecordMapper.selectList(
            new LambdaQueryWrapper<FinanceRecord>()
                .eq(FinanceRecord::getDeleted, 0)
                .ge(FinanceRecord::getCreateTime, startDate.atStartOfDay())
                .le(FinanceRecord::getCreateTime, endDate.atTime(23, 59, 59))
        );

        // 按 period 分组
        Map<String, List<FinanceRecord>> grouped = records.stream().collect(Collectors.groupingBy(r -> {
            LocalDate d = r.getCreateTime().toLocalDate();
            if ("weekly".equals(period)) {
                return d.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).toString();
            } else if ("monthly".equals(period)) {
                return d.withDayOfMonth(1).toString();
            }
            return d.toString();
        }));

        List<Map<String, Object>> trend = new ArrayList<>();
        for (Map.Entry<String, List<FinanceRecord>> entry : grouped.entrySet()) {
            BigDecimal income = entry.getValue().stream()
                .filter(r -> "income".equals(r.getType()))
                .map(FinanceRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal expense = entry.getValue().stream()
                .filter(r -> "expense".equals(r.getType()))
                .map(FinanceRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            // 生成短标签
            String label = entry.getKey().substring(5); // 去掉年份，保留 MM-DD
            if ("weekly".equals(period) && label.length() >= 5) {
                label = label.substring(0, 5) + " 周";
            }
            if ("monthly".equals(period) && label.length() >= 2) {
                label = label.substring(0, 2) + "月";
            }
            trend.add(Map.of(
                "label", label,
                "date", entry.getKey(),
                "income", income,
                "expense", expense
            ));
        }
        trend.sort(Comparator.comparing(m -> m.get("date").toString()));
        return Result.ok(trend);
    }

    // ========== 流水导出 ==========

    @GetMapping("/flow/export")
    @Operation(summary = "导出流水统计")
    public void exportFlow(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_OK);
    }

    @GetMapping("/order/report/export")
    @Operation(summary = "导出订单统计报表")
    public void exportOrderReport(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_OK);
    }

    @GetMapping("/material/export")
    @Operation(summary = "导出物料统计")
    public void exportMaterial(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_OK);
    }
}
