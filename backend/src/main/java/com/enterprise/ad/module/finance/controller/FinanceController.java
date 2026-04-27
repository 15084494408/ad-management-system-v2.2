package com.enterprise.ad.module.finance.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.ad.common.PageResult;
import com.enterprise.ad.common.Result;
import com.enterprise.ad.module.finance.entity.FinanceRecord;
import com.enterprise.ad.module.finance.entity.FinanceQuote;
import com.enterprise.ad.module.finance.entity.FinanceInvoice;
import com.enterprise.ad.module.finance.mapper.FinanceRecordMapper;
import com.enterprise.ad.module.finance.mapper.FinanceQuoteMapper;
import com.enterprise.ad.module.finance.mapper.FinanceInvoiceMapper;
import com.enterprise.ad.module.order.entity.Order;
import com.enterprise.ad.module.order.mapper.OrderMapper;
import com.enterprise.ad.module.factory.entity.FactoryBill;
import com.enterprise.ad.module.factory.mapper.FactoryBillMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/finance")
@RequiredArgsConstructor
@Tag(name = "财务管理")
public class FinanceController {

    private final FinanceRecordMapper financeRecordMapper;
    private final FinanceQuoteMapper quoteMapper;
    private final FinanceInvoiceMapper invoiceMapper;
    private final OrderMapper orderMapper;
    private final FactoryBillMapper factoryBillMapper;

    @GetMapping("/records")
    @Operation(summary = "财务流水列表")
    @PreAuthorize("hasAuthority('finance:view')")
    public Result<PageResult<FinanceRecord>> listRecords(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        Page<FinanceRecord> page = new Page<>(current, size);
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(23, 59, 59) : null;
        LambdaQueryWrapper<FinanceRecord> qw = new LambdaQueryWrapper<FinanceRecord>()
            .eq(type != null, FinanceRecord::getType, type)
            .eq(category != null, FinanceRecord::getCategory, category)
            .ge(startDateTime != null, FinanceRecord::getCreateTime, startDateTime)
            .le(endDateTime != null, FinanceRecord::getCreateTime, endDateTime)
            .eq(FinanceRecord::getDeleted, 0)
            .orderByDesc(FinanceRecord::getCreateTime);
        Page<FinanceRecord> result = financeRecordMapper.selectPage(page, qw);
        return Result.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @PostMapping("/records")
    @Operation(summary = "新增财务记录（快速记账）")
    @PreAuthorize("hasAuthority('finance:edit')")
    public Result<Long> createRecord(@RequestBody FinanceRecord record) {
        // ★ 修复：金额校验
        if (record.getAmount() == null || record.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail(400, "金额必须大于0");
        }
        if (record.getType() == null || record.getType().isBlank()) {
            return Result.fail(400, "类型不能为空");
        }

        record.setRecordNo("FIN" + System.currentTimeMillis());
        record.setCreateTime(LocalDateTime.now());
        record.setDeleted(0);
        financeRecordMapper.insert(record);
        return Result.ok(record.getId());
    }

    @PutMapping("/records/{id}")
    @Operation(summary = "更新财务记录")
    @PreAuthorize("hasAuthority('finance:edit')")
    public Result<Void> updateRecord(@PathVariable Long id, @RequestBody FinanceRecord record) {
        record.setId(id);
        if (record.getAmount() != null && record.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail(400, "金额必须大于0");
        }
        financeRecordMapper.updateById(record);
        return Result.ok();
    }

    @DeleteMapping("/records/{id}")
    @Operation(summary = "删除财务记录")
    @PreAuthorize("hasAuthority('finance:edit')")
    public Result<Void> deleteRecord(@PathVariable Long id) {
        // ★ 修复：deleteById 在 @TableLogic 下会自动转为逻辑删除
        financeRecordMapper.deleteById(id);
        return Result.ok();
    }

    /**
     * ★ 新增：财务概览（前端 financeApi.getOverview 需要的接口）
     * 与 /summary 合并，统一返回概览数据
     */
    @GetMapping("/overview")
    @Operation(summary = "财务概览（前端首页使用）")
    @PreAuthorize("hasAuthority('finance:view')")
    public Result<Map<String, Object>> getOverview(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        // 默认查本月
        if (startDate == null) startDate = LocalDate.now().withDayOfMonth(1);
        if (endDate == null) endDate = LocalDate.now();

        Map<String, Object> data = buildSummaryData(startDate, endDate);
        return Result.ok(data);
    }

    @GetMapping("/summary")
    @Operation(summary = "收支统计摘要")
    @PreAuthorize("hasAuthority('finance:view')")
    public Result<Map<String, Object>> getSummary(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        if (startDate == null) startDate = LocalDate.now().withDayOfMonth(1);
        if (endDate == null) endDate = LocalDate.now();

        Map<String, Object> data = buildSummaryData(startDate, endDate);
        return Result.ok(data);
    }

    @GetMapping("/quick-records")
    @Operation(summary = "快捷记账列表（最近10条）")
    @PreAuthorize("hasAuthority('finance:view')")
    public Result<List<FinanceRecord>> getQuickRecords() {
        List<FinanceRecord> records = financeRecordMapper.selectList(
            new LambdaQueryWrapper<FinanceRecord>()
                .eq(FinanceRecord::getDeleted, 0)
                .orderByDesc(FinanceRecord::getCreateTime)
                .last("LIMIT 10")
        );
        return Result.ok(records);
    }

    /**
     * 构建收支统计数据
     */
    private Map<String, Object> buildSummaryData(LocalDate startDate, LocalDate endDate) {
        // 收入合计
        BigDecimal income = financeRecordMapper.selectList(
            new LambdaQueryWrapper<FinanceRecord>()
                .eq(FinanceRecord::getType, "income")
                .ge(FinanceRecord::getCreateTime, startDate.atStartOfDay())
                .le(FinanceRecord::getCreateTime, endDate.atTime(23, 59, 59))
                .eq(FinanceRecord::getDeleted, 0)
        ).stream().map(FinanceRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        // 支出合计
        BigDecimal expense = financeRecordMapper.selectList(
            new LambdaQueryWrapper<FinanceRecord>()
                .eq(FinanceRecord::getType, "expense")
                .ge(FinanceRecord::getCreateTime, startDate.atStartOfDay())
                .le(FinanceRecord::getCreateTime, endDate.atTime(23, 59, 59))
                .eq(FinanceRecord::getDeleted, 0)
        ).stream().map(FinanceRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("income", income);
        summary.put("expense", expense);
        summary.put("profit", income.subtract(expense));
        summary.put("startDate", startDate);
        summary.put("endDate", endDate);
        return summary;
    }

    // ========== 应收应付 ==========

    @GetMapping("/arap/summary")
    @Operation(summary = "应收应付汇总")
    @PreAuthorize("hasAuthority('finance:view')")
    public Result<Map<String, Object>> arapSummary() {
        // 应收：订单总额 - 已付金额
        List<Order> allOrders = orderMapper.selectList(
            new LambdaQueryWrapper<Order>().eq(Order::getDeleted, 0));
        BigDecimal receivableTotal = allOrders.stream().map(Order::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal receivedTotal = allOrders.stream().map(Order::getPaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        // 应付：工厂账单
        List<FactoryBill> bills = factoryBillMapper.selectList(
            new LambdaQueryWrapper<FactoryBill>().eq(FactoryBill::getDeleted, 0));
        BigDecimal payableTotal = bills.stream().map(FactoryBill::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paidTotal = bills.stream().map(b -> b.getPaidAmount() != null ? b.getPaidAmount() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);

        return Result.ok(Map.of(
            "receivableTotal", receivableTotal,
            "receivedTotal", receivedTotal,
            "payableTotal", payableTotal,
            "overdueTotal", payableTotal.subtract(paidTotal)
        ));
    }

    @GetMapping("/arap/receivable")
    @Operation(summary = "应收明细列表")
    @PreAuthorize("hasAuthority('finance:view')")
    public Result<PageResult<Order>> receivableList(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String status) {
        Page<Order> page = new Page<>(current, size);
        LambdaQueryWrapper<Order> qw = new LambdaQueryWrapper<Order>()
            .eq(Order::getDeleted, 0)
            .like(customerName != null, Order::getCustomerName, customerName)
            .orderByDesc(Order::getCreateTime);
        Page<Order> result = orderMapper.selectPage(page, qw);
        return Result.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @GetMapping("/arap/payable")
    @Operation(summary = "应付明细列表")
    @PreAuthorize("hasAuthority('finance:view')")
    public Result<PageResult<FactoryBill>> payableList(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String supplier,
            @RequestParam(required = false) String status) {
        Page<FactoryBill> page = new Page<>(current, size);
        LambdaQueryWrapper<FactoryBill> qw = new LambdaQueryWrapper<FactoryBill>()
            .eq(FactoryBill::getDeleted, 0)
            .like(supplier != null, FactoryBill::getFactoryName, supplier)
            .orderByDesc(FactoryBill::getCreateTime);
        Page<FactoryBill> result = factoryBillMapper.selectPage(page, qw);
        return Result.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    // ========== 报价管理 ==========

    @GetMapping("/quote/list")
    @Operation(summary = "报价列表")
    @PreAuthorize("hasAuthority('finance:view')")
    public Result<PageResult<FinanceQuote>> quoteList(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String status) {
        Page<FinanceQuote> page = new Page<>(current, size);
        LambdaQueryWrapper<FinanceQuote> qw = new LambdaQueryWrapper<FinanceQuote>()
            .eq(FinanceQuote::getDeleted, 0)
            .like(customerName != null, FinanceQuote::getCustomerName, customerName)
            .eq(status != null, FinanceQuote::getStatus, status)
            .orderByDesc(FinanceQuote::getCreateTime);
        Page<FinanceQuote> result = quoteMapper.selectPage(page, qw);
        return Result.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @PostMapping("/quote/save")
    @Operation(summary = "新建/更新报价")
    @PreAuthorize("hasAuthority('finance:edit')")
    public Result<Long> saveQuote(@RequestBody FinanceQuote quote) {
        if (quote.getId() != null) {
            quote.setUpdateTime(LocalDateTime.now());
            quoteMapper.updateById(quote);
            return Result.ok(quote.getId());
        }
        quote.setQuoteNo("QT" + System.currentTimeMillis());
        if (quote.getStatus() == null) quote.setStatus("pending");
        if (quote.getDiscount() == null) quote.setDiscount(BigDecimal.valueOf(100));
        if (quote.getTotalAmount() != null && quote.getDiscount() != null) {
            quote.setFinalAmount(quote.getTotalAmount().multiply(quote.getDiscount()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        }
        quote.setCreateTime(LocalDateTime.now());
        quoteMapper.insert(quote);
        return Result.ok(quote.getId());
    }

    // ========== 发票管理 ==========

    @GetMapping("/invoice/list")
    @Operation(summary = "发票列表")
    @PreAuthorize("hasAuthority('finance:view')")
    public Result<PageResult<FinanceInvoice>> invoiceList(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String invoiceNo,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        Page<FinanceInvoice> page = new Page<>(current, size);
        LambdaQueryWrapper<FinanceInvoice> qw = new LambdaQueryWrapper<FinanceInvoice>()
            .eq(FinanceInvoice::getDeleted, 0)
            .like(invoiceNo != null, FinanceInvoice::getInvoiceNo, invoiceNo)
            .like(customerName != null, FinanceInvoice::getCustomerName, customerName)
            .eq(type != null, FinanceInvoice::getType, type)
            .eq(status != null, FinanceInvoice::getStatus, status)
            .orderByDesc(FinanceInvoice::getCreateTime);
        Page<FinanceInvoice> result = invoiceMapper.selectPage(page, qw);
        return Result.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @PostMapping("/invoice/save")
    @Operation(summary = "新建/更新发票")
    @PreAuthorize("hasAuthority('finance:edit')")
    public Result<Long> saveInvoice(@RequestBody FinanceInvoice invoice) {
        if (invoice.getId() != null) {
            invoice.setUpdateTime(LocalDateTime.now());
            invoiceMapper.updateById(invoice);
            return Result.ok(invoice.getId());
        }
        invoice.setInvoiceNo("FP" + System.currentTimeMillis());
        if (invoice.getStatus() == null) invoice.setStatus("pending");
        if (invoice.getTaxRate() != null && invoice.getAmount() != null) {
            invoice.setTaxAmount(invoice.getAmount().multiply(invoice.getTaxRate()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        }
        invoice.setCreateTime(LocalDateTime.now());
        invoiceMapper.insert(invoice);
        return Result.ok(invoice.getId());
    }

    @PutMapping("/invoice/{id}/issue")
    @Operation(summary = "开具发票")
    @PreAuthorize("hasAuthority('finance:edit')")
    public Result<Void> issueInvoice(@PathVariable Long id) {
        FinanceInvoice invoice = new FinanceInvoice();
        invoice.setId(id);
        invoice.setStatus("completed");
        invoice.setIssueDate(LocalDate.now().toString());
        invoice.setUpdateTime(LocalDateTime.now());
        invoiceMapper.updateById(invoice);
        return Result.ok();
    }

    @PutMapping("/invoice/{id}/cancel")
    @Operation(summary = "作废发票")
    @PreAuthorize("hasAuthority('finance:edit')")
    public Result<Void> cancelInvoice(@PathVariable Long id) {
        FinanceInvoice invoice = new FinanceInvoice();
        invoice.setId(id);
        invoice.setStatus("cancelled");
        invoice.setUpdateTime(LocalDateTime.now());
        invoiceMapper.updateById(invoice);
        return Result.ok();
    }

    // ========== 财务报表 ==========

    @GetMapping("/report/summary")
    @Operation(summary = "财务报表摘要（总收入/总支出/净利润/利润率）")
    @PreAuthorize("hasAuthority('finance:view')")
    public Result<Map<String, Object>> reportSummary(
            @RequestParam(required = false) String reportType,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        if (startDate == null) startDate = LocalDate.now().withDayOfMonth(1);
        if (endDate == null) endDate = LocalDate.now();

        Map<String, Object> summary = buildSummaryData(startDate, endDate);
        BigDecimal income = (BigDecimal) summary.get("income");
        BigDecimal expense = (BigDecimal) summary.get("expense");
        BigDecimal profit = income.subtract(expense);
        int profitRate = income.compareTo(BigDecimal.ZERO) > 0
            ? profit.multiply(BigDecimal.valueOf(100)).divide(income, 0, RoundingMode.HALF_UP).intValue()
            : 0;

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("totalIncome", income);
        report.put("totalExpense", expense);
        report.put("totalProfit", profit);
        report.put("profitRate", profitRate);
        return Result.ok(report);
    }

    // ========== 流水统计 ==========

    @GetMapping("/flow/statistics")
    @Operation(summary = "流水统计（收入合计/支出合计/净流水/笔数）")
    @PreAuthorize("hasAuthority('finance:view')")
    public Result<Map<String, Object>> flowStatistics(
            @RequestParam(defaultValue = "day") String period,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(required = false) String flowType) {
        if (startDate == null) startDate = LocalDate.now().minusDays(7);
        if (endDate == null) endDate = LocalDate.now();

        LambdaQueryWrapper<FinanceRecord> qw = new LambdaQueryWrapper<FinanceRecord>()
            .eq(FinanceRecord::getDeleted, 0)
            .ge(FinanceRecord::getCreateTime, startDate.atStartOfDay())
            .le(FinanceRecord::getCreateTime, endDate.atTime(23, 59, 59));

        List<FinanceRecord> records = financeRecordMapper.selectList(qw);
        BigDecimal income = records.stream().filter(r -> "income".equals(r.getType())).map(FinanceRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expense = records.stream().filter(r -> "expense".equals(r.getType())).map(FinanceRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        return Result.ok(Map.of(
            "totalIncome", income,
            "totalExpense", expense,
            "netAmount", income.subtract(expense),
            "recordCount", records.size()
        ));
    }

    // ========== 统一流水（聚合所有收支来源） ==========

    @GetMapping("/all-flow")
    @Operation(summary = "统一流水列表（聚合快速记账+会员充值消费+订单收款+工厂付款）")
    @PreAuthorize("hasAuthority('finance:view')")
    public Result<Map<String, Object>> allFlow(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(required = false) String direction) {
        String startStr = startDate != null ? startDate.atStartOfDay().toString() : null;
        String endStr = endDate != null ? endDate.atTime(23, 59, 59).toString() : null;
        long offset = (current - 1) * size;

        long total = financeRecordMapper.countAllFlow(startStr, endStr, direction);
        List<Map<String, Object>> records = financeRecordMapper.selectAllFlow(startStr, endStr, direction, size, offset);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("current", current);
        result.put("size", size);
        result.put("records", records);
        return Result.ok(result);
    }

    // ========== 导出接口（简化版，返回提示） ==========

    @GetMapping("/arap/export")
    @Operation(summary = "导出应收应付")
    public void exportArap(HttpServletResponse response) {
        // 简化：前端直接 window.open 调用，实际生产环境应生成 Excel 文件
        response.setStatus(HttpServletResponse.SC_OK);
    }

    @GetMapping("/quote/export")
    @Operation(summary = "导出报价")
    public void exportQuote(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_OK);
    }

    @GetMapping("/invoice/export")
    @Operation(summary = "导出发票")
    public void exportInvoice(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_OK);
    }

    @GetMapping("/report/export")
    @Operation(summary = "导出财务报表")
    public void exportReport(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_OK);
    }

    @GetMapping("/flow/export")
    @Operation(summary = "导出流水")
    public void exportFlow(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_OK);
    }

    @GetMapping("/flow/statistics/export")
    @Operation(summary = "导出流水统计")
    public void exportFlowStatistics(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_OK);
    }
}
