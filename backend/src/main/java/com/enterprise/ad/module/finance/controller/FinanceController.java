package com.enterprise.ad.module.finance.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.ad.common.PageResult;
import com.enterprise.ad.common.Result;
import com.enterprise.ad.common.annotation.OperationLog;
import com.enterprise.ad.common.dto.QuoteStatusRequest;
import com.enterprise.ad.common.util.CsvExportUtil;
import com.enterprise.ad.common.util.DateUtil;
import com.enterprise.ad.module.finance.dto.CreateQuoteRequest;
import com.enterprise.ad.module.finance.dto.CreateRecordRequest;
import com.enterprise.ad.module.finance.entity.FinanceRecord;
import com.enterprise.ad.module.finance.entity.FinanceRecordItem;
import com.enterprise.ad.module.finance.entity.FinanceQuote;
import com.enterprise.ad.module.finance.entity.FinanceInvoice;
import com.enterprise.ad.module.finance.entity.FinQuoteDetail;
import com.enterprise.ad.module.finance.mapper.FinanceRecordMapper;
import com.enterprise.ad.module.finance.mapper.FinanceRecordItemMapper;
import com.enterprise.ad.module.finance.mapper.FinanceQuoteMapper;
import com.enterprise.ad.module.finance.mapper.FinanceInvoiceMapper;
import com.enterprise.ad.module.finance.mapper.FinQuoteDetailMapper;
import com.enterprise.ad.module.material.entity.Material;
import com.enterprise.ad.module.material.entity.StockLog;
import com.enterprise.ad.module.material.mapper.MaterialMapper;
import com.enterprise.ad.module.material.mapper.StockLogMapper;
import com.enterprise.ad.module.customer.entity.Customer;
import com.enterprise.ad.module.customer.mapper.CustomerMapper;
import com.enterprise.ad.module.order.entity.Order;
import com.enterprise.ad.module.order.mapper.OrderMapper;
import com.enterprise.ad.module.factory.entity.FactoryBill;
import com.enterprise.ad.module.factory.mapper.FactoryBillMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/finance")
@RequiredArgsConstructor
@Tag(name = "财务管理")
public class FinanceController {

    private final FinanceRecordMapper financeRecordMapper;
    private final FinanceRecordItemMapper recordItemMapper;
    private final FinanceQuoteMapper quoteMapper;
    private final FinanceInvoiceMapper invoiceMapper;
    private final FinQuoteDetailMapper quoteDetailMapper;
    private final OrderMapper orderMapper;
    private final FactoryBillMapper factoryBillMapper;
    private final MaterialMapper materialMapper;
    private final StockLogMapper stockLogMapper;
    private final CustomerMapper customerMapper;

    @GetMapping("/records")
    @Operation(summary = "财务流水列表")
    @PreAuthorize("hasAuthority('finance:view')")
    public Result<PageResult<FinanceRecord>> listRecords(
            @RequestParam(defaultValue = "1") long current,
            // ★ 修复 P2-9: 分页上限校验
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        // ★ 分页上限校验
        size = Math.min(size, 100);
        // ★ 空字符串视为不筛选
        if (type != null && type.isBlank()) type = null;
        if (category != null && category.isBlank()) category = null;

        Page<FinanceRecord> page = new Page<>(current, size);
        // ★ 使用 DateUtil 统一日期转换
        LocalDateTime startDateTime = DateUtil.startOfDay(startDate);
        LocalDateTime endDateTime = DateUtil.endOfDay(endDate);
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
    @Operation(summary = "新增财务记录（快速记账，支持物料明细+库存联动）")
    @OperationLog(value = "快速记账", module = "财务管理")
    @PreAuthorize("hasAuthority('finance:edit')")
    @Transactional
    public Result<Long> createRecord(@Valid @RequestBody CreateRecordRequest request) {
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail(400, "金额必须大于0");
        }
        FinanceRecord record = createFinanceRecord(request);
        if (record == null) return Result.fail(500, "创建财务记录失败");
        processRecordItems(request.getItems(), record);
        return Result.ok(record.getId());
    }

    /** 创建财务流水主记录 */
    private FinanceRecord createFinanceRecord(CreateRecordRequest request) {
        FinanceRecord record = new FinanceRecord();
        record.setRecordNo("FIN" + System.currentTimeMillis());
        record.setType(request.getType());
        record.setCategory(request.getCategory());
        record.setAmount(request.getAmount());
        record.setRelatedName(request.getRelatedName());
        record.setPaymentMethod(request.getPaymentMethod());
        record.setRemark(request.getRemark());
        record.setCreateTime(LocalDateTime.now());
        record.setDeleted(0);
        financeRecordMapper.insert(record);
        return record;
    }

    /** 处理物料明细及库存出库联动 */
    private void processRecordItems(List<CreateRecordRequest.RecordItemRequest> items, FinanceRecord record) {
        if (items == null || items.isEmpty()) return;
        for (CreateRecordRequest.RecordItemRequest item : items) {
            saveRecordItem(item, record.getId());
            processStockOut(item, record);
        }
    }

    /** 保存物料明细 */
    private void saveRecordItem(CreateRecordRequest.RecordItemRequest item, Long recordId) {
        FinanceRecordItem recordItem = new FinanceRecordItem();
        recordItem.setRecordId(recordId);
        recordItem.setMaterialId(item.getMaterialId());
        recordItem.setMaterialName(item.getMaterialName());
        recordItem.setPricingType(item.getPricingType() != null ? item.getPricingType() : 0);
        recordItem.setQuantity(item.getQuantity());
        recordItem.setWidth(item.getWidth());
        recordItem.setHeight(item.getHeight());
        recordItem.setArea(item.getArea());
        recordItem.setUnitPrice(item.getUnitPrice());
        recordItem.setTotalPrice(item.getTotalPrice());
        recordItem.setCreateTime(LocalDateTime.now());
        recordItem.setDeleted(0);
        recordItemMapper.insert(recordItem);
    }

    /** 执行库存出库 */
    private void processStockOut(CreateRecordRequest.RecordItemRequest item, FinanceRecord record) {
        Material material = materialMapper.selectById(item.getMaterialId());
        if (material == null || material.getDeleted() != 0) return;

        int qty = calcStockOutQty(item);
        if (qty <= 0) return;

        int before = material.getStockQuantity();
        int after = before - qty;

        logStockChange(material, qty, before, after, record);
        material.setStockQuantity(after);
        material.setUpdateTime(LocalDateTime.now());
        materialMapper.updateById(material);
        syncPaperGroupStock(material, after);
    }

    /** 计算出库数量 */
    private int calcStockOutQty(CreateRecordRequest.RecordItemRequest item) {
        if (item.getQuantity() != null && item.getQuantity() > 0) return item.getQuantity();
        if (item.getPricingType() != null && item.getPricingType() == 1 && item.getArea() != null) {
            return item.getArea().setScale(0, RoundingMode.CEILING).intValue();
        }
        return 0;
    }

    /** 记录库存变动日志 */
    private void logStockChange(Material material, int qty, int before, int after, FinanceRecord record) {
        StockLog log = new StockLog();
        log.setMaterialId(material.getId());
        log.setMaterialName(material.getName());
        log.setChangeType(2);
        log.setQuantity(-qty);
        log.setBeforeStock(before);
        log.setAfterStock(after);
        log.setUnitPrice(material.getPrice());
        log.setTotalPrice(material.getPrice().multiply(new BigDecimal(qty)));
        log.setRemark("快速记账关联出库：" + record.getRecordNo());
        log.setCreateTime(LocalDateTime.now());
        stockLogMapper.insert(log);
    }

    @Deprecated // [P2-01] 前端未调用此接口，编辑财务记录后只能删重建
    @PutMapping("/records/{id}")
    @Operation(summary = "更新财务记录（已弃用）")
    @PreAuthorize("hasAuthority('finance:edit')")
    public Result<Void> updateRecord(@PathVariable Long id, @Valid @RequestBody FinanceRecord record) {
        record.setId(id);
        if (record.getAmount() != null && record.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail(400, "金额必须大于0");
        }
        financeRecordMapper.updateById(record);
        return Result.ok();
    }

    @DeleteMapping("/records/{id}")
    @Operation(summary = "删除财务记录")
    @OperationLog(value = "删除财务记录", module = "财务管理")
    @PreAuthorize("hasAuthority('finance:edit')")
    public Result<Void> deleteRecord(@PathVariable Long id) {
        financeRecordMapper.deleteById(id);
        return Result.ok();
    }

    @GetMapping("/overview")
    @Operation(summary = "财务概览（前端首页使用）")
    @PreAuthorize("hasAuthority('finance:view')")
    public Result<Map<String, Object>> getOverview(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        LocalDate[] range = DateUtil.fillMonthRange(startDate, endDate);
        LocalDateTime monthStart = range[0].atStartOfDay();
        LocalDateTime monthEnd = range[1].atTime(23, 59, 59);

        // 本月收入
        BigDecimal thisMonthIncome = financeRecordMapper.sumIncomeByRange(monthStart, monthEnd);
        
        // 待收款 = 所有未付清订单的（总额 - 已付 - 抹零）
        BigDecimal unpaidAmount = orderMapper.sumUnpaidAmount();
        
        // 会员余额总计
        BigDecimal memberBalance = customerMapper.selectList(
            new LambdaQueryWrapper<Customer>()
                .eq(Customer::getDeleted, 0)
                .gt(Customer::getBalance, BigDecimal.ZERO)
        ).stream().map(Customer::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 本月订单数
        long thisMonthOrders = orderMapper.selectCount(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getDeleted, 0)
                .ge(Order::getCreateTime, monthStart)
                .le(Order::getCreateTime, monthEnd));
        
        // 近30天收款趋势（补齐每一天，无数据填0）
        List<Map<String, Object>> dailyData = financeRecordMapper.selectDailyIncomeLast30Days();
        // 构建日期→金额映射
        Map<String, BigDecimal> dayMap = new HashMap<>();
        for (Map<String, Object> row : dailyData) {
            Object dateObj = row.get("date");
            Object amtObj = row.get("daily_income");
            if (dateObj != null) {
                String dayStr = dateObj.toString();
                BigDecimal amt = amtObj instanceof BigDecimal ? (BigDecimal) amtObj
                    : new BigDecimal(amtObj != null ? amtObj.toString() : "0");
                dayMap.put(dayStr, amt);
            }
        }
        // 生成最近30天的序列
        List<BigDecimal> trendData = new ArrayList<>();
        List<String> trendLabels = new ArrayList<>();   // 日期标签
        List<BigDecimal> trendAmounts = new ArrayList<>(); // 实际金额
        LocalDate today = LocalDate.now();
        DateTimeFormatter labelFmt = DateTimeFormatter.ofPattern("MM/dd");
        for (int i = 29; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            String key = d.toString();
            BigDecimal amt = dayMap.getOrDefault(key, BigDecimal.ZERO);
            trendData.add(amt);
            trendLabels.add(d.format(labelFmt));
            trendAmounts.add(amt);
        }
        BigDecimal maxVal = trendData.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ONE);
        // 归一化为百分比（0~100）
        List<Integer> trendPercent = new ArrayList<>();
        for (BigDecimal v : trendData) {
            int pct = maxVal.compareTo(BigDecimal.ZERO) > 0
                ? v.multiply(new BigDecimal("100")).divide(maxVal, 0, RoundingMode.HALF_UP).intValue()
                : 0;
            trendPercent.add(Math.max(pct, 4)); // 最低4px占位
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("thisMonthIncome", thisMonthIncome);
        data.put("unpaidAmount", unpaidAmount);
        data.put("memberBalance", memberBalance);
        data.put("thisMonthOrders", thisMonthOrders);
        data.put("trendData", trendPercent);       // 柱高百分比
        data.put("trendLabels", trendLabels);       // 日期标签
        data.put("trendAmounts", trendAmounts);     // 实际金额
        return Result.ok(data);
    }

    @Deprecated // [P2-01] 前端未调用此接口，概览数据由 /overview 和 /board 提供
    @GetMapping("/summary")
    @Operation(summary = "收支统计摘要（已弃用）")
    @PreAuthorize("hasAuthority('finance:view')")
    public Result<Map<String, Object>> getSummary(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        LocalDate[] range = DateUtil.fillMonthRange(startDate, endDate);

        Map<String, Object> data = buildSummaryData(range[0], range[1]);
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
     * ★ 修复: 构建收支统计数据 — 改用 SQL 聚合查询，不再全量加载到 Java 内存
     */
    private Map<String, Object> buildSummaryData(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        // ★ SQL 聚合，不再全量加载
        BigDecimal income = financeRecordMapper.sumIncomeByRange(start, end);
        BigDecimal expense = financeRecordMapper.sumExpenseByRange(start, end);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("income", income);
        summary.put("expense", expense);
        summary.put("profit", income.subtract(expense));
        summary.put("startDate", startDate);
        summary.put("endDate", endDate);
        return summary;
    }

    // ========== 应收应付 ==========

    /**
     * ★ 修复 P1-11: 应收应付汇总 — 改用 SQL 聚合查询
     */
    @GetMapping("/arap/summary")
    @Operation(summary = "应收应付汇总")
    @PreAuthorize("hasAuthority('finance:view')")
    public Result<Map<String, Object>> arapSummary() {
        // ★ 使用 SQL 聚合查询，不再全表加载到内存
        BigDecimal receivableTotal = orderMapper.sumAllTotalAmount();
        BigDecimal receivedTotal = orderMapper.sumAllPaidAmount();
        BigDecimal payableTotal = factoryBillMapper.sumAllTotalAmount();
        BigDecimal paidTotal = factoryBillMapper.sumAllPaidAmount();

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
            // ★ 修复 P2-9: 分页上限校验
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String status) {
        if (customerName != null && customerName.isBlank()) customerName = null;
        if (status != null && status.isBlank()) status = null;
        // ★ 分页上限校验
        size = Math.min(size, 100);
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
        if (supplier != null && supplier.isBlank()) supplier = null;
        if (status != null && status.isBlank()) status = null;
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
        // ★ 空字符串视为不筛选，避免 LIKE '' 或 = '' 导致查不到数据
        if (customerName != null && customerName.isBlank()) customerName = null;
        if (status != null && status.isBlank()) status = null;

        Page<FinanceQuote> page = new Page<>(current, size);
        LambdaQueryWrapper<FinanceQuote> qw = new LambdaQueryWrapper<FinanceQuote>()
            .eq(FinanceQuote::getDeleted, 0)
            .like(customerName != null, FinanceQuote::getCustomerName, customerName)
            .eq(status != null, FinanceQuote::getStatus, status)
            .orderByDesc(FinanceQuote::getCreateTime);
        Page<FinanceQuote> result = quoteMapper.selectPage(page, qw);
        return Result.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @GetMapping("/quote/{id}")
    @Operation(summary = "报价详情（含明细）")
    @PreAuthorize("hasAuthority('finance:view')")
    public Result<Map<String, Object>> quoteDetail(@PathVariable Long id) {
        FinanceQuote quote = quoteMapper.selectById(id);
        if (quote == null) {
            return Result.fail(404, "报价不存在");
        }
        List<FinQuoteDetail> details = quoteDetailMapper.selectList(
            new LambdaQueryWrapper<FinQuoteDetail>()
                .eq(FinQuoteDetail::getQuoteId, id)
                .eq(FinQuoteDetail::getDeleted, 0)
                .orderByAsc(FinQuoteDetail::getId)
        );
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("quote", quote);
        data.put("details", details);
        return Result.ok(data);
    }

    /**
     * ★ 修复 P1-10: 新建报价 — 改用类型安全的 DTO 替代 Map<String, Object>
     */
    @PostMapping("/quote/create")
    @Operation(summary = "新建报价（主表+明细同时提交）")
    @PreAuthorize("hasAuthority('finance:edit')")
    public Result<Long> createQuote(@Valid @RequestBody CreateQuoteRequest request) {
        FinanceQuote quote = new FinanceQuote();
        // 生成编号：QT + 日期 + 3位序列
        String dateStr = LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        long seq = System.currentTimeMillis() % 1000;
        quote.setQuoteNo("QT" + dateStr + String.format("%03d", seq));

        quote.setCustomerName(request.getCustomerName());
        quote.setCustomerId(request.getCustomerId());
        quote.setProjectName(request.getProjectName());
        quote.setCompanyId(request.getCompanyId());
        quote.setDiscount(request.getDiscount() != null ? request.getDiscount() : BigDecimal.valueOf(100));
        quote.setTaxRate(request.getTaxRate());
        quote.setValidUntil(request.getValidUntil());
        quote.setQuoteDate(request.getQuoteDate());
        quote.setRemark(request.getRemark());
        quote.setTotalAmount(request.getTotalAmount());
        quote.setFinalAmount(request.getFinalAmount());
        quote.setTaxAmount(request.getTaxAmount());

        quote.setStatus("pending");
        quote.setCreateTime(LocalDateTime.now());
        quote.setDeleted(0);
        quoteMapper.insert(quote);

        // 插入明细
        if (request.getDetails() != null && !request.getDetails().isEmpty()) {
            for (CreateQuoteRequest.QuoteDetailRequest d : request.getDetails()) {
                FinQuoteDetail detail = new FinQuoteDetail();
                detail.setQuoteId(quote.getId());
                detail.setMaterialName(d.getMaterialName());
                detail.setSpec(d.getSpec());
                detail.setUnit(d.getUnit());
                detail.setQuantity(d.getQuantity());
                detail.setUnitPrice(d.getUnitPrice());
                detail.setAmount(d.getAmount());
                detail.setRemark(d.getRemark());
                detail.setIsCustom(d.getIsCustom() != null ? d.getIsCustom() : 0);
                detail.setCreateTime(LocalDateTime.now());
                detail.setDeleted(0);
                quoteDetailMapper.insert(detail);
            }
        }

        return Result.ok(quote.getId());
    }

    /**
     * ★ 修复 P2-15: 删除报价 — 使用批量删除替代循环 N+1 删除
     */
    @DeleteMapping("/quote/{id}")
    @Operation(summary = "删除报价（含明细）")
    @PreAuthorize("hasAuthority('finance:edit')")
    public Result<Void> deleteQuote(@PathVariable Long id) {
        quoteMapper.deleteById(id);
        // ★ 批量删除明细，不再循环逐条删除
        List<FinQuoteDetail> details = quoteDetailMapper.selectList(
            new LambdaQueryWrapper<FinQuoteDetail>().eq(FinQuoteDetail::getQuoteId, id)
        );
        if (!details.isEmpty()) {
            List<Long> detailIds = details.stream().map(FinQuoteDetail::getId).collect(Collectors.toList());
            quoteDetailMapper.deleteBatchIds(detailIds);
        }
        return Result.ok();
    }

    @PutMapping("/quote/{id}/status")
    @Operation(summary = "变更报价状态")
    @PreAuthorize("hasAuthority('finance:edit')")
    public Result<Void> updateQuoteStatus(@PathVariable Long id, @Valid @RequestBody QuoteStatusRequest body) {
        String status = body.getStatus();
        FinanceQuote update = new FinanceQuote();
        update.setId(id);
        update.setStatus(status);
        update.setUpdateTime(LocalDateTime.now());
        quoteMapper.updateById(update);
        return Result.ok();
    }

    @PostMapping("/quote/save")
    @Operation(summary = "新建/更新报价")
    @PreAuthorize("hasAuthority('finance:edit')")
    public Result<Long> saveQuote(@Valid @RequestBody FinanceQuote quote) {
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
        if (invoiceNo != null && invoiceNo.isBlank()) invoiceNo = null;
        if (customerName != null && customerName.isBlank()) customerName = null;
        if (type != null && type.isBlank()) type = null;
        if (status != null && status.isBlank()) status = null;
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
    public Result<Long> saveInvoice(@Valid @RequestBody FinanceInvoice invoice) {
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
    @Operation(summary = "财务报表（汇总+月度明细+趋势+支出分类）")
    @PreAuthorize("hasAuthority('finance:view')")
    public Result<Map<String, Object>> reportSummary(
            @RequestParam(required = false) String reportType,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        LocalDate[] reportRange = DateUtil.fillMonthRange(startDate, endDate);

        Map<String, Object> summary = buildSummaryData(reportRange[0], reportRange[1]);
        BigDecimal income = (BigDecimal) summary.get("income");
        BigDecimal expense = (BigDecimal) summary.get("expense");
        BigDecimal profit = income.subtract(expense);
        int profitRate = income.compareTo(BigDecimal.ZERO) > 0
            ? profit.multiply(BigDecimal.valueOf(100)).divide(income, 0, RoundingMode.HALF_UP).intValue()
            : 0;

        // ---- 查询范围内所有财务记录 ----
        LocalDateTime rangeStart = reportRange[0].atStartOfDay();
        LocalDateTime rangeEnd = reportRange[1].atTime(23, 59, 59);

        List<FinanceRecord> allRecords = financeRecordMapper.selectList(
            new LambdaQueryWrapper<FinanceRecord>()
                .eq(FinanceRecord::getDeleted, 0)
                .ge(FinanceRecord::getCreateTime, rangeStart)
                .le(FinanceRecord::getCreateTime, rangeEnd));

        // 按月分组：月度明细 + 收支趋势
        Map<String, Map<String, Object>> monthMap = new LinkedHashMap<>();
        DateTimeFormatter ymFmt = DateTimeFormatter.ofPattern("yyyy-MM");
        for (FinanceRecord r : allRecords) {
            String ym = r.getCreateTime() != null ? r.getCreateTime().format(ymFmt) : "unknown";
            monthMap.putIfAbsent(ym, new LinkedHashMap<>());
            Map<String, Object> m = monthMap.get(ym);
            m.put("month", ym);
            BigDecimal inc = (BigDecimal) m.getOrDefault("income", BigDecimal.ZERO);
            BigDecimal exp = (BigDecimal) m.getOrDefault("expense", BigDecimal.ZERO);
            if ("income".equals(r.getType())) {
                m.put("income", inc.add(r.getAmount() != null ? r.getAmount() : BigDecimal.ZERO));
            } else if ("expense".equals(r.getType())) {
                m.put("expense", exp.add(r.getAmount() != null ? r.getAmount() : BigDecimal.ZERO));
            }
        }

        // 查询每月订单数
        QueryWrapper<Order> orderQw = new QueryWrapper<Order>()
            .eq("deleted", 0)
            .ge("create_time", rangeStart)
            .le("create_time", rangeEnd)
            .select("DATE_FORMAT(create_time, '%Y-%m') as month, COUNT(*) as count")
            .groupBy("month");
        List<Map<String, Object>> monthlyOrders = orderMapper.selectMaps(orderQw);

        List<Map<String, Object>> monthlyList = new ArrayList<>();
        List<Map<String, Object>> trendData = new ArrayList<>();
        for (Map.Entry<String, Map<String, Object>> entry : monthMap.entrySet()) {
            String ym = entry.getKey();
            Map<String, Object> m = entry.getValue();
            BigDecimal mIncome = (BigDecimal) m.getOrDefault("income", BigDecimal.ZERO);
            BigDecimal mExpense = (BigDecimal) m.getOrDefault("expense", BigDecimal.ZERO);
            BigDecimal mProfit = mIncome.subtract(mExpense);
            int mProfitRate = mIncome.compareTo(BigDecimal.ZERO) > 0
                ? mProfit.multiply(BigDecimal.valueOf(100)).divide(mIncome, 0, RoundingMode.HALF_UP).intValue()
                : 0;
            // 订单数
            long orderCount = monthlyOrders.stream()
                .filter(o -> ym.equals(o.get("month")))
                .mapToLong(o -> ((Number) o.get("count")).longValue())
                .findFirst().orElse(0);

            monthlyList.add(Map.of(
                "month", ym, "income", mIncome, "expense", mExpense,
                "profit", mProfit, "profitRate", mProfitRate,
                "orderCount", orderCount, "newCustomers", 0
            ));
            trendData.add(Map.of("month", ym, "income", mIncome, "expense", mExpense));
        }

        // 支出分类
        Map<String, BigDecimal> catMap = new LinkedHashMap<>();
        for (FinanceRecord r : allRecords) {
            if ("expense".equals(r.getType()) && r.getCategory() != null) {
                catMap.merge(r.getCategory(), r.getAmount() != null ? r.getAmount() : BigDecimal.ZERO, BigDecimal::add);
            }
        }
        List<Map<String, Object>> expenseCategories = new ArrayList<>();
        String[] colors = {"#409eff","#67c23a","#e6a23c","#f56c6c","#909399","#b37feb","#5cdbd3","#ff85c0"};
        int ci = 0;
        for (Map.Entry<String, BigDecimal> e : catMap.entrySet()) {
            expenseCategories.add(Map.of(
                "label", e.getKey(), "amount", e.getValue(),
                "category", e.getKey(), "color", colors[ci % colors.length]
            ));
            ci++;
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("totalIncome", income);
        report.put("totalExpense", expense);
        report.put("totalProfit", profit);
        report.put("profitRate", profitRate);
        report.put("trendData", trendData);
        report.put("expenseCategories", expenseCategories);
        report.put("monthlyList", monthlyList);
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
        LocalDate[] flowRange = DateUtil.fillWeekRange(startDate, endDate);

        LocalDateTime start = flowRange[0].atStartOfDay();
        LocalDateTime end = flowRange[1].atTime(23, 59, 59);

        // ★ SQL 聚合
        LambdaQueryWrapper<FinanceRecord> countQw = new LambdaQueryWrapper<FinanceRecord>()
            .eq(FinanceRecord::getDeleted, 0)
            .ge(FinanceRecord::getCreateTime, start)
            .le(FinanceRecord::getCreateTime, end);

        long recordCount = financeRecordMapper.selectCount(countQw);
        BigDecimal income = financeRecordMapper.sumIncomeByRange(start, end);
        BigDecimal expense = financeRecordMapper.sumExpenseByRange(start, end);

        return Result.ok(Map.of(
            "totalIncome", income,
            "totalExpense", expense,
            "netAmount", income.subtract(expense),
            "recordCount", recordCount
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

    // ========== ★ 修复 P2-16: 导出接口实现 ==========

    @GetMapping("/arap/export")
    @Operation(summary = "导出应收应付（CSV）")
    @PreAuthorize("hasAuthority('finance:view')")
    public void exportArap(HttpServletResponse response) throws IOException {
        CsvExportUtil.setExportHeaders(response, "应收应付数据");

        StringBuilder csv = new StringBuilder();
        csv.append("类型,编号,客户/供应商,金额,已付金额,未付金额,创建时间\n");

        // 应收订单
        List<Order> orders = orderMapper.selectList(
            new LambdaQueryWrapper<Order>().eq(Order::getDeleted, 0).orderByDesc(Order::getCreateTime));
        for (Order o : orders) {
            BigDecimal total = o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal paid = o.getPaidAmount() != null ? o.getPaidAmount() : BigDecimal.ZERO;
            csv.append(String.format("应收,%s,%s,%s,%s,%s,%s\n",
                CsvExportUtil.escapeCsv(o.getOrderNo()), CsvExportUtil.escapeCsv(o.getCustomerName()),
                total, paid, total.subtract(paid),
                o.getCreateTime() != null ? o.getCreateTime().toString() : ""));
        }

        // 应付工厂账单
        List<FactoryBill> bills = factoryBillMapper.selectList(
            new LambdaQueryWrapper<FactoryBill>().eq(FactoryBill::getDeleted, 0).orderByDesc(FactoryBill::getCreateTime));
        for (FactoryBill b : bills) {
            BigDecimal total = b.getTotalAmount() != null ? b.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal paid = b.getPaidAmount() != null ? b.getPaidAmount() : BigDecimal.ZERO;
            csv.append(String.format("应付,%s,%s,%s,%s,%s,%s\n",
                CsvExportUtil.escapeCsv(b.getBillNo()), CsvExportUtil.escapeCsv(b.getFactoryName()),
                total, paid, total.subtract(paid),
                b.getCreateTime() != null ? b.getCreateTime().toString() : ""));
        }

        response.getWriter().write(csv.toString());
    }

    @GetMapping("/quote/export")
    @Operation(summary = "导出报价（CSV）")
    @PreAuthorize("hasAuthority('finance:view')")
    public void exportQuote(HttpServletResponse response) throws IOException {
        CsvExportUtil.setExportHeaders(response, "报价数据");

        StringBuilder csv = new StringBuilder();
        csv.append("报价编号,客户名称,项目名称,合计金额,折扣率,最终金额,税额,状态,报价日期,有效期至\n");

        List<FinanceQuote> quotes = quoteMapper.selectList(
            new LambdaQueryWrapper<FinanceQuote>().eq(FinanceQuote::getDeleted, 0).orderByDesc(FinanceQuote::getCreateTime));
        for (FinanceQuote q : quotes) {
            csv.append(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                CsvExportUtil.escapeCsv(q.getQuoteNo()), CsvExportUtil.escapeCsv(q.getCustomerName()), CsvExportUtil.escapeCsv(q.getProjectName()),
                q.getTotalAmount(), q.getDiscount(), q.getFinalAmount(), q.getTaxAmount(),
                CsvExportUtil.escapeCsv(q.getStatus()), CsvExportUtil.escapeCsv(q.getQuoteDate()), CsvExportUtil.escapeCsv(q.getValidUntil())));
        }

        response.getWriter().write(csv.toString());
    }

    @GetMapping("/invoice/export")
    @Operation(summary = "导出发票（CSV）")
    @PreAuthorize("hasAuthority('finance:view')")
    public void exportInvoice(HttpServletResponse response) throws IOException {
        CsvExportUtil.setExportHeaders(response, "发票数据");

        StringBuilder csv = new StringBuilder();
        csv.append("发票编号,类型,客户名称,金额,税率,税额,状态,开票日期\n");

        List<FinanceInvoice> invoices = invoiceMapper.selectList(
            new LambdaQueryWrapper<FinanceInvoice>().eq(FinanceInvoice::getDeleted, 0).orderByDesc(FinanceInvoice::getCreateTime));
        for (FinanceInvoice inv : invoices) {
            csv.append(String.format("%s,%s,%s,%s,%s,%s,%s,%s\n",
                CsvExportUtil.escapeCsv(inv.getInvoiceNo()), CsvExportUtil.escapeCsv(inv.getType()), CsvExportUtil.escapeCsv(inv.getCustomerName()),
                inv.getAmount(), inv.getTaxRate(), inv.getTaxAmount(),
                CsvExportUtil.escapeCsv(inv.getStatus()), CsvExportUtil.escapeCsv(inv.getIssueDate())));
        }

        response.getWriter().write(csv.toString());
    }

    @GetMapping("/report/export")
    @Operation(summary = "导出财务报表（CSV）")
    @PreAuthorize("hasAuthority('finance:view')")
    public void exportReport(HttpServletResponse response) throws IOException {
        CsvExportUtil.setExportHeaders(response, "财务报表");

        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);

        Map<String, Object> summary = buildSummaryData(startOfMonth, today);

        StringBuilder csv = new StringBuilder();
        csv.append("项目,金额\n");
        csv.append(String.format("总收入,%s\n", summary.get("income")));
        csv.append(String.format("总支出,%s\n", summary.get("expense")));
        csv.append(String.format("净利润,%s\n", summary.get("profit")));
        csv.append(String.format("统计周期,%s 至 %s\n", summary.get("startDate"), summary.get("endDate")));

        response.getWriter().write(csv.toString());
    }

    @GetMapping("/flow/export")
    @Operation(summary = "导出流水（CSV）")
    @PreAuthorize("hasAuthority('finance:view')")
    public void exportFlow(HttpServletResponse response) throws IOException {
        CsvExportUtil.setExportHeaders(response, "流水数据");

        StringBuilder csv = new StringBuilder();
        csv.append("来源,编号,方向,金额,分类,相关方,备注,创建时间\n");

        LocalDate today = LocalDate.now();
        String startStr = today.minusDays(30).atStartOfDay().toString();
        String endStr = today.atTime(23, 59, 59).toString();

        List<Map<String, Object>> records = financeRecordMapper.selectAllFlow(startStr, endStr, null, 10000, 0);
        for (Map<String, Object> r : records) {
            csv.append(String.format("%s,%s,%s,%s,%s,%s,%s,%s\n",
                CsvExportUtil.escapeCsv(String.valueOf(r.get("source"))), CsvExportUtil.escapeCsv(String.valueOf(r.get("record_no"))),
                CsvExportUtil.escapeCsv(String.valueOf(r.get("direction"))), r.get("amount"),
                CsvExportUtil.escapeCsv(String.valueOf(r.get("category"))), CsvExportUtil.escapeCsv(String.valueOf(r.get("related_name"))),
                CsvExportUtil.escapeCsv(String.valueOf(r.get("remark"))), CsvExportUtil.escapeCsv(String.valueOf(r.get("create_time")))));
        }

        response.getWriter().write(csv.toString());
    }

    @GetMapping("/flow/statistics/export")
    @Operation(summary = "导出流水统计（CSV）")
    @PreAuthorize("hasAuthority('finance:view')")
    public void exportFlowStatistics(HttpServletResponse response) throws IOException {
        CsvExportUtil.setExportHeaders(response, "流水统计");

        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(30);
        LocalDateTime lStart = start.atStartOfDay();
        LocalDateTime lEnd = today.atTime(23, 59, 59);

        BigDecimal income = financeRecordMapper.sumIncomeByRange(lStart, lEnd);
        BigDecimal expense = financeRecordMapper.sumExpenseByRange(lStart, lEnd);

        StringBuilder csv = new StringBuilder();
        csv.append("项目,金额\n");
        csv.append(String.format("总收入,%s\n", income));
        csv.append(String.format("总支出,%s\n", expense));
        csv.append(String.format("净流水,%s\n", income.subtract(expense)));
        csv.append(String.format("统计周期,%s 至 %s\n", start, today));

        response.getWriter().write(csv.toString());
    }

    // ========== 流水物料明细 ==========

    @GetMapping("/records/{recordId}/items")
    @Operation(summary = "获取流水物料明细")
    @PreAuthorize("hasAuthority('finance:view')")
    public Result<List<FinanceRecordItem>> getRecordItems(@PathVariable Long recordId) {
        List<FinanceRecordItem> items = recordItemMapper.selectList(
            new LambdaQueryWrapper<FinanceRecordItem>()
                .eq(FinanceRecordItem::getRecordId, recordId)
                .eq(FinanceRecordItem::getDeleted, 0)
                .orderByAsc(FinanceRecordItem::getId)
        );
        return Result.ok(items);
    }

    // ========== 纸张分组库存同步 ==========

    /**
     * 同步同纸张分组（paperGroup）的其他物料的库存数量
     * 同一 paperGroup（同纸张类型+材质）的黑白/彩色共享库存
     */
    private void syncPaperGroupStock(Material source, int newStock) {
        String paperGroup = source.getPaperGroup();
        if (paperGroup == null || paperGroup.isBlank()) {
            return;
        }
        List<Material> siblings = materialMapper.selectList(
            new LambdaQueryWrapper<Material>()
                .eq(Material::getPaperGroup, paperGroup)
                .eq(Material::getDeleted, 0)
                .ne(Material::getId, source.getId())
        );
        LocalDateTime now = LocalDateTime.now();
        for (Material m : siblings) {
            m.setStockQuantity(newStock);
            m.setUpdateTime(now);
            materialMapper.updateById(m);
        }
    }

}
