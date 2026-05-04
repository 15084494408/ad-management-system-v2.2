package com.enterprise.ad.module.dashboard.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.ad.common.Result;
import com.enterprise.ad.common.util.DateUtil;
import com.enterprise.ad.module.customer.entity.Customer;
import com.enterprise.ad.module.customer.mapper.CustomerMapper;
import com.enterprise.ad.module.finance.entity.FinanceRecord;
import com.enterprise.ad.module.finance.mapper.FinanceRecordMapper;
import com.enterprise.ad.module.material.entity.Material;
import com.enterprise.ad.module.material.mapper.MaterialMapper;
import com.enterprise.ad.module.member.entity.Member;
import com.enterprise.ad.module.member.mapper.MemberMapper;
import com.enterprise.ad.module.order.entity.Order;
import com.enterprise.ad.module.order.mapper.OrderMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 仪表盘（数据统计）
 */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "仪表盘")
public class DashboardController {

    private final OrderMapper orderMapper;
    private final CustomerMapper customerMapper;
    private final MemberMapper memberMapper;
    private final FinanceRecordMapper financeRecordMapper;
    private final MaterialMapper materialMapper;

    @GetMapping("/stats")
    @Operation(summary = "核心指标统计（仪表盘全量数据）")
    @PreAuthorize("hasAuthority('dashboard:view')")
    public Result<Map<String, Object>> getStats() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(23, 59, 59);
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();

        Map<String, Object> stats = new LinkedHashMap<>();

        // 今日新增订单
        long todayOrders = orderMapper.selectCount(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getDeleted, 0)
                .ge(Order::getCreateTime, todayStart)
                .le(Order::getCreateTime, todayEnd));
        stats.put("todayOrders", todayOrders);

        // 今日营收 = 今日财务流水（收入类，已包含订单收款和快速记账，避免双重计算）
        BigDecimal todayRevenue = financeRecordMapper.selectList(
            new LambdaQueryWrapper<FinanceRecord>()
                .eq(FinanceRecord::getDeleted, 0)
                .eq(FinanceRecord::getType, "income")
                .ge(FinanceRecord::getCreateTime, todayStart)
                .le(FinanceRecord::getCreateTime, todayEnd)
        ).stream().map(FinanceRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("todayRevenue", todayRevenue);

        // 客户总数
        long totalCustomers = customerMapper.selectCount(
            new LambdaQueryWrapper<Customer>().eq(Customer::getDeleted, 0));
        stats.put("totalCustomers", totalCustomers);

        // 库存预警（库存 <= 预警量）
        long stockWarnings = materialMapper.selectCount(
            new LambdaQueryWrapper<Material>()
                .eq(Material::getDeleted, 0)
                .apply("stock_quantity <= warning_quantity"));
        stats.put("stockWarnings", stockWarnings);

        // 订单趋势（较昨日）
        long yesterdayOrders = orderMapper.selectCount(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getDeleted, 0)
                .ge(Order::getCreateTime, today.minusDays(1).atStartOfDay())
                .le(Order::getCreateTime, today.minusDays(1).atTime(23, 59, 59)));
        int orderTrend = yesterdayOrders > 0
            ? (int) ((todayOrders - yesterdayOrders) * 100.0 / yesterdayOrders)
            : (todayOrders > 0 ? 100 : 0);
        stats.put("orderTrend", orderTrend);

        // 营收趋势（较昨日，仅使用 fin_record 避免双重计算）
        BigDecimal yesterdayRevenue = financeRecordMapper.selectList(
            new LambdaQueryWrapper<FinanceRecord>()
                .eq(FinanceRecord::getDeleted, 0)
                .eq(FinanceRecord::getType, "income")
                .ge(FinanceRecord::getCreateTime, today.minusDays(1).atStartOfDay())
                .le(FinanceRecord::getCreateTime, today.minusDays(1).atTime(23, 59, 59))
        ).stream().map(FinanceRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        int revenueTrend = yesterdayRevenue.compareTo(BigDecimal.ZERO) > 0
            ? todayRevenue.subtract(yesterdayRevenue).multiply(new BigDecimal("100"))
                .divide(yesterdayRevenue, 0, RoundingMode.HALF_UP).intValue()
            : (todayRevenue.compareTo(BigDecimal.ZERO) > 0 ? 100 : 0);
        stats.put("revenueTrend", revenueTrend);

        // 本月新增客户
        long newCustomers = customerMapper.selectCount(
            new LambdaQueryWrapper<Customer>()
                .eq(Customer::getDeleted, 0)
                .ge(Customer::getCreateTime, monthStart));
        stats.put("newCustomers", newCustomers);

        // 今日流水（今日所有收支记录总额）
        List<FinanceRecord> todayRecords = financeRecordMapper.selectList(
            new LambdaQueryWrapper<FinanceRecord>()
                .eq(FinanceRecord::getDeleted, 0)
                .ge(FinanceRecord::getCreateTime, todayStart)
                .le(FinanceRecord::getCreateTime, todayEnd));
        BigDecimal todayFlow = todayRecords.stream()
            .map(FinanceRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal todayExpense = todayRecords.stream()
            .filter(r -> "expense".equals(r.getType()))
            .map(FinanceRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("todayFlow", todayFlow);
        stats.put("todayTxCount", todayRecords.size());
        stats.put("todayExpense", todayExpense);

        // 本周流水
        BigDecimal weekFlow = financeRecordMapper.selectList(
            new LambdaQueryWrapper<FinanceRecord>()
                .eq(FinanceRecord::getDeleted, 0)
                .ge(FinanceRecord::getCreateTime, weekStart)
        ).stream().map(FinanceRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("weekFlow", weekFlow);

        // 本月流水
        BigDecimal monthFlow = financeRecordMapper.selectList(
            new LambdaQueryWrapper<FinanceRecord>()
                .eq(FinanceRecord::getDeleted, 0)
                .ge(FinanceRecord::getCreateTime, monthStart)
        ).stream().map(FinanceRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("monthFlow", monthFlow);

        // 本月营收（收入类型记录之和）
        BigDecimal monthRevenue = financeRecordMapper.selectList(
            new LambdaQueryWrapper<FinanceRecord>()
                .eq(FinanceRecord::getType, "income")
                .eq(FinanceRecord::getDeleted, 0)
                .ge(FinanceRecord::getCreateTime, monthStart)
        ).stream().map(FinanceRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("monthRevenue", monthRevenue);

        // 营收构成（按支付方式分组）
        Map<String, BigDecimal> channelMap = new LinkedHashMap<>();
        financeRecordMapper.selectList(
            new LambdaQueryWrapper<FinanceRecord>()
                .eq(FinanceRecord::getType, "income")
                .eq(FinanceRecord::getDeleted, 0)
                .ge(FinanceRecord::getCreateTime, monthStart)
        ).forEach(r -> {
            String ch = r.getPaymentMethod() != null ? r.getPaymentMethod() : "其他";
            channelMap.merge(ch, r.getAmount(), BigDecimal::add);
        });
        // 按支付方式名称映射颜色
        java.util.Map<String, String> channelColorMap = new java.util.LinkedHashMap<>();
        channelColorMap.put("微信支付", "#07c160");
        channelColorMap.put("微信", "#07c160");
        channelColorMap.put("支付宝", "#1677ff");
        channelColorMap.put("现金", "#ff6b6b");
        channelColorMap.put("转账", "#722ed1");
        channelColorMap.put("其他", "#909399");
        List<Map<String, Object>> breakdown = new ArrayList<>();
        BigDecimal monthRevenueFinal = monthRevenue;
        for (Map.Entry<String, BigDecimal> entry : channelMap.entrySet()) {
            BigDecimal pct = monthRevenueFinal.compareTo(BigDecimal.ZERO) > 0
                ? entry.getValue().multiply(new BigDecimal("100")).divide(monthRevenueFinal, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", entry.getKey());
            item.put("amount", entry.getValue());
            item.put("percent", pct);
            item.put("color", channelColorMap.getOrDefault(entry.getKey(), "#909399"));
            breakdown.add(item);
        }
        stats.put("revenueBreakdown", breakdown);

        // 会员总数
        long memberCount = memberMapper.selectCount(
            new LambdaQueryWrapper<Member>()
                .eq(Member::getDeleted, 0)
                .eq(Member::getStatus, 1));
        stats.put("memberCount", memberCount);

        return Result.ok(stats);
    }

    @GetMapping("/charts/orderTrend")
    @Operation(summary = "订单趋势图（近7天每日订单量）")
    @PreAuthorize("hasAuthority('dashboard:view')")
    public Result<List<Map<String, Object>>> getOrderTrend() {
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            String dateStr = date.format(DateTimeFormatter.ofPattern("MM-dd"));
            long count = orderMapper.selectCount(
                new LambdaQueryWrapper<Order>()
                    .eq(Order::getDeleted, 0)
                    .ge(Order::getCreateTime, date.atStartOfDay())
                    .le(Order::getCreateTime, date.atTime(23, 59, 59)));
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", dateStr);
            item.put("count", count);
            trend.add(item);
        }
        return Result.ok(trend);
    }

    @GetMapping("/board")
    @Operation(summary = "数据看板聚合接口（科技风大屏）")
    @PreAuthorize("hasAuthority('dashboard:view')")
    public Result<Map<String, Object>> getBoard() {
        LocalDate today = LocalDate.now();
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = today.atTime(23, 59, 59);

        // KPI 数据
        BigDecimal thisMonthIncome = BigDecimal.ZERO;
        List<FinanceRecord> monthIncomeList = financeRecordMapper.selectList(
            new LambdaQueryWrapper<FinanceRecord>()
                .eq(FinanceRecord::getType, "income")
                .eq(FinanceRecord::getDeleted, 0)
                .ge(FinanceRecord::getCreateTime, monthStart)
                .le(FinanceRecord::getCreateTime, monthEnd));
        for (FinanceRecord r : monthIncomeList) {
            thisMonthIncome = thisMonthIncome.add(r.getAmount() != null ? r.getAmount() : BigDecimal.ZERO);
        }

        Long thisMonthOrders = orderMapper.countThisMonthOrders(monthStart, monthEnd);
        if (thisMonthOrders == null) thisMonthOrders = 0L;

        BigDecimal unpaidAmount = orderMapper.sumUnpaidAmount();
        if (unpaidAmount == null) unpaidAmount = BigDecimal.ZERO;

        BigDecimal orderCost = orderMapper.sumOrderCostByRange(monthStart, monthEnd);
        if (orderCost == null) orderCost = BigDecimal.ZERO;

        // 利润率 = (收入 - 订单总成本) / 收入 × 100%
        BigDecimal profitRate = BigDecimal.ZERO;
        if (thisMonthIncome.compareTo(BigDecimal.ZERO) > 0) {
            profitRate = thisMonthIncome.subtract(orderCost)
                .multiply(new BigDecimal("100"))
                .divide(thisMonthIncome, 1, RoundingMode.HALF_UP);
        }

        // 利润金额
        BigDecimal profitAmount = thisMonthIncome.subtract(orderCost).max(BigDecimal.ZERO);

        // 本年收款
        LocalDateTime yearStart = today.withDayOfYear(1).atStartOfDay();
        BigDecimal thisYearIncome = BigDecimal.ZERO;
        List<FinanceRecord> yearIncomeList = financeRecordMapper.selectList(
            new LambdaQueryWrapper<FinanceRecord>()
                .eq(FinanceRecord::getType, "income")
                .eq(FinanceRecord::getDeleted, 0)
                .ge(FinanceRecord::getCreateTime, yearStart)
                .le(FinanceRecord::getCreateTime, monthEnd));
        for (FinanceRecord r : yearIncomeList) {
            thisYearIncome = thisYearIncome.add(r.getAmount() != null ? r.getAmount() : BigDecimal.ZERO);
        }

        // 客户总数
        long totalCustomers = customerMapper.selectCount(
            new LambdaQueryWrapper<Customer>().eq(Customer::getDeleted, 0));

        // 30天收款趋势
        List<Map<String, Object>> dailyIncome = financeRecordMapper.selectDailyIncomeLast30Days();
        List<Integer> incomeTrend = new ArrayList<>();
        List<BigDecimal> incomeTrendAmounts = new ArrayList<>();
        if (dailyIncome != null) {
            for (Map<String, Object> row : dailyIncome) {
                Object count = row.get("count");
                Object amount = row.get("amount");
                incomeTrend.add(count instanceof Number ? ((Number) count).intValue() : 0);
                incomeTrendAmounts.add(amount instanceof BigDecimal ? (BigDecimal) amount
                    : amount instanceof Number ? BigDecimal.valueOf(((Number) amount).doubleValue())
                    : BigDecimal.ZERO);
            }
        }

        // 未完成订单（status=1待处理, 2进行中），含收款状态
        List<Order> unfinishedList = orderMapper.selectList(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getDeleted, 0)
                .in(Order::getStatus, 1, 2)
                .orderByAsc(Order::getStatus)
                .last("LIMIT 20"));
        List<Map<String, Object>> unfinishedOrders = new ArrayList<>();
        for (Order o : unfinishedList) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", o.getId());
            item.put("orderNo", o.getOrderNo());
            item.put("customerName", o.getCustomerName());
            item.put("title", o.getTitle());
            item.put("totalAmount", o.getTotalAmount());
            item.put("paidAmount", o.getPaidAmount());
            item.put("status", o.getStatus());
            BigDecimal unpaid = BigDecimal.ZERO;
            if (o.getTotalAmount() != null) {
                unpaid = o.getTotalAmount()
                    .subtract(o.getPaidAmount() != null ? o.getPaidAmount() : BigDecimal.ZERO)
                    .subtract(o.getRoundingAmount() != null ? o.getRoundingAmount() : BigDecimal.ZERO)
                    .subtract(o.getDiscountAmount() != null ? o.getDiscountAmount() : BigDecimal.ZERO)
                    .max(BigDecimal.ZERO);
            }
            item.put("unpaidAmount", unpaid);
            item.put("paymentStatus", o.getPaymentStatus());
            unfinishedOrders.add(item);
        }

        // 最近10条流水
        List<FinanceRecord> records = financeRecordMapper.selectList(
            new LambdaQueryWrapper<FinanceRecord>()
                .eq(FinanceRecord::getDeleted, 0)
                .orderByDesc(FinanceRecord::getCreateTime)
                .last("LIMIT 10"));
        List<Map<String, Object>> recentRecords = new ArrayList<>();
        for (FinanceRecord r : records) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", r.getId());
            item.put("recordNo", r.getRecordNo());
            item.put("type", r.getType());
            item.put("category", r.getCategory());
            item.put("amount", r.getAmount());
            item.put("relatedName", r.getRelatedName());
            item.put("createTime", r.getCreateTime());
            recentRecords.add(item);
        }

        // 订单状态分布
        long pendingCount = orderMapper.selectCount(
            new LambdaQueryWrapper<Order>().eq(Order::getDeleted, 0).eq(Order::getStatus, 1));
        long processingCount = orderMapper.selectCount(
            new LambdaQueryWrapper<Order>().eq(Order::getDeleted, 0).eq(Order::getStatus, 2));
        long completedCount = orderMapper.selectCount(
            new LambdaQueryWrapper<Order>().eq(Order::getDeleted, 0).eq(Order::getStatus, 3));
        long cancelledCount = orderMapper.selectCount(
            new LambdaQueryWrapper<Order>().eq(Order::getDeleted, 0).eq(Order::getStatus, 4));
        Map<String, Object> orderDist = new LinkedHashMap<>();
        orderDist.put("pending", pendingCount);
        orderDist.put("processing", processingCount);
        orderDist.put("completed", completedCount);
        orderDist.put("cancelled", cancelledCount);

        // 组装响应
        Map<String, Object> kpi = new LinkedHashMap<>();
        kpi.put("thisMonthIncome", thisMonthIncome);
        kpi.put("thisYearIncome", thisYearIncome);
        kpi.put("profitAmount", profitAmount);
        kpi.put("thisMonthOrders", thisMonthOrders);
        kpi.put("unpaidAmount", unpaidAmount);
        kpi.put("profitRate", profitRate);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("kpi", kpi);
        result.put("totalCustomers", totalCustomers);
        result.put("incomeTrend", incomeTrend);
        result.put("incomeTrendAmounts", incomeTrendAmounts);
        result.put("unfinishedOrders", unfinishedOrders);
        result.put("recentRecords", recentRecords);
        result.put("orderStatusDist", orderDist);

        // 客户消费排行 TOP10
        List<Customer> topCustomers = customerMapper.selectList(
            new LambdaQueryWrapper<Customer>()
                .eq(Customer::getDeleted, 0)
                .ne(Customer::getTotalAmount, BigDecimal.ZERO)
                .isNotNull(Customer::getTotalAmount)
                .orderByDesc(Customer::getTotalAmount)
                .last("LIMIT 10"));
        List<Map<String, Object>> topCustomerList = new ArrayList<>();
        int rank = 1;
        for (Customer c : topCustomers) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("rank", rank++);
            item.put("id", c.getId());
            item.put("customerName", c.getCustomerName());
            item.put("totalAmount", c.getTotalAmount() != null ? c.getTotalAmount() : BigDecimal.ZERO);
            item.put("orderCount", c.getOrderCount() != null ? c.getOrderCount() : 0);
            item.put("level", c.getLevel() != null ? c.getLevel() : 1);
            topCustomerList.add(item);
        }
        result.put("topCustomers", topCustomerList);

        return Result.ok(result);
    }
}
