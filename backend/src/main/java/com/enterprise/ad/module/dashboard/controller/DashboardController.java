package com.enterprise.ad.module.dashboard.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.ad.common.Result;
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

        // 今日营收 = 今日完成订单金额 + 今日快速记账（收入类）
        BigDecimal todayOrderRevenue = orderMapper.selectList(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getDeleted, 0)
                .eq(Order::getStatus, 3)
                .ge(Order::getCreateTime, todayStart)
                .le(Order::getCreateTime, todayEnd)
        ).stream().map(o -> o.getPaidAmount() != null ? o.getPaidAmount() : BigDecimal.ZERO)
         .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal todayQuickIncome = financeRecordMapper.selectList(
            new LambdaQueryWrapper<FinanceRecord>()
                .eq(FinanceRecord::getDeleted, 0)
                .eq(FinanceRecord::getType, "income")
                .ge(FinanceRecord::getCreateTime, todayStart)
                .le(FinanceRecord::getCreateTime, todayEnd)
        ).stream().map(FinanceRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal todayRevenue = todayOrderRevenue.add(todayQuickIncome);
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

        // 营收趋势（较昨日，同样包含快速记账收入）
        BigDecimal yesterdayOrderRevenue = orderMapper.selectList(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getDeleted, 0)
                .eq(Order::getStatus, 3)
                .ge(Order::getCreateTime, today.minusDays(1).atStartOfDay())
                .le(Order::getCreateTime, today.minusDays(1).atTime(23, 59, 59))
        ).stream().map(o -> o.getPaidAmount() != null ? o.getPaidAmount() : BigDecimal.ZERO)
         .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal yesterdayQuickIncome = financeRecordMapper.selectList(
            new LambdaQueryWrapper<FinanceRecord>()
                .eq(FinanceRecord::getDeleted, 0)
                .eq(FinanceRecord::getType, "income")
                .ge(FinanceRecord::getCreateTime, today.minusDays(1).atStartOfDay())
                .le(FinanceRecord::getCreateTime, today.minusDays(1).atTime(23, 59, 59))
        ).stream().map(FinanceRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal yesterdayRevenue = yesterdayOrderRevenue.add(yesterdayQuickIncome);
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
        stats.put("todayFlow", todayFlow);
        stats.put("todayTxCount", todayRecords.size());

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
        String[] colorArr = {"#409eff", "#67c23a", "#e6a23c", "#f56c6c", "#909399"};
        String[] nameArr = {"微信", "支付宝", "现金", "转账", "其他"};
        List<Map<String, Object>> breakdown = new ArrayList<>();
        int idx = 0;
        BigDecimal monthRevenueFinal = monthRevenue;
        for (Map.Entry<String, BigDecimal> entry : channelMap.entrySet()) {
            BigDecimal pct = monthRevenueFinal.compareTo(BigDecimal.ZERO) > 0
                ? entry.getValue().multiply(new BigDecimal("100")).divide(monthRevenueFinal, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
            String name = nameArr[idx] != null ? nameArr[idx] : entry.getKey();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", name);
            item.put("amount", entry.getValue());
            item.put("percent", pct);
            item.put("color", colorArr[idx % colorArr.length]);
            breakdown.add(item);
            idx++;
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
}
