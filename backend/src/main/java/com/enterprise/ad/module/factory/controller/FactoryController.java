package com.enterprise.ad.module.factory.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.ad.common.PageResult;
import com.enterprise.ad.common.Result;
import com.enterprise.ad.module.factory.entity.FactoryBill;
import com.enterprise.ad.module.factory.entity.FactoryBillDetail;
import com.enterprise.ad.module.customer.mapper.CustomerMapper;
import com.enterprise.ad.module.customer.entity.Customer;
import com.enterprise.ad.module.factory.mapper.FactoryBillMapper;
import com.enterprise.ad.module.factory.mapper.FactorySalesmanMapper;
import com.enterprise.ad.module.factory.entity.FactorySalesman;
import com.enterprise.ad.module.factory.mapper.FactoryBillDetailMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/factory")
@RequiredArgsConstructor
@Tag(name = "工厂管理")
public class FactoryController {

    private final FactoryBillMapper factoryBillMapper;
    private final FactoryBillDetailMapper factoryBillDetailMapper;
    private final CustomerMapper customerMapper;
    private final FactorySalesmanMapper factorySalesmanMapper;

    // ========== 工厂管理 ==========

    /**
     * 工厂列表（从客户表中筛选 customer_type=2 的记录）
     * 注意：fac_factory 表已在 v2.2 合并到 crm_customer，不再使用
     */
    @GetMapping("/list")
    @Operation(summary = "工厂列表（从客户表获取工厂客户）")
    @PreAuthorize("hasAuthority('factory:list')")
    public Result<List<Customer>> listFactories() {
        // 从客户表查询工厂客户（customer_type=2）
        List<Customer> factoryCustomers = customerMapper.selectList(
            new LambdaQueryWrapper<Customer>()
                .eq(Customer::getDeleted, 0)
                .eq(Customer::getCustomerType, Customer.TYPE_FACTORY)
                .orderByAsc(Customer::getCustomerName));
        
        return Result.ok(factoryCustomers);
    }

    // ========== 工厂账单 ==========
    // 同时支持客户账单（billType=2），共用同一张表

    @GetMapping("/bills")
    @Operation(summary = "账单列表（支持按类型/客户/月份/状态筛选）")
    @PreAuthorize("hasAuthority('factory:list')")
    public Result<PageResult<FactoryBill>> listBills(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Integer billType,
            @RequestParam(required = false) Long factoryId,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) Integer status) {
        Page<FactoryBill> page = new Page<>(current, size);
        LambdaQueryWrapper<FactoryBill> qw = new LambdaQueryWrapper<FactoryBill>()
            // 按账单类型筛选（1=工厂 2=客户），不传则查全部
            .eq(billType != null, FactoryBill::getBillType, billType)
            // 使用 customer_id 筛选（而非 factory_id），因为 customer_id 存储的是 crm_customer 的正确 ID
            .eq(factoryId != null, FactoryBill::getCustomerId, factoryId)
            .eq(status != null, FactoryBill::getStatus, status)
            .like(month != null, FactoryBill::getMonth, month)
            .eq(FactoryBill::getDeleted, 0)
            .orderByDesc(FactoryBill::getCreateTime);
        Page<FactoryBill> result = factoryBillMapper.selectPage(page, qw);

        // ★ 修复：列表接口不应有写操作，客户名称校正值仅返回给前端，不再回写数据库
        for (FactoryBill bill : result.getRecords()) {
            if (bill.getCustomerId() != null) {
                Customer customer = customerMapper.selectById(bill.getCustomerId());
                if (customer != null && !customer.getCustomerName().equals(bill.getFactoryName())) {
                    bill.setFactoryName(customer.getCustomerName());
                }
            }
        }

        return Result.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @GetMapping("/bills/{id}")
    @Operation(summary = "账单详情")
    @PreAuthorize("hasAuthority('factory:list')")
    public Result<FactoryBill> getBill(@PathVariable Long id) {
        FactoryBill bill = factoryBillMapper.selectById(id);
        if (bill == null || bill.getDeleted() == 1) {
            return Result.fail("账单不存在");
        }
        return Result.ok(bill);
    }

    @PostMapping("/bills")
    @Operation(summary = "新建账单（自动从客户ID关联客户信息）")
    @PreAuthorize("hasAuthority('factory:create')")
    public Result<Long> createBill(@RequestBody FactoryBill bill) {
        // 0. 默认账单类型：未指定时为工厂账单
        if (bill.getBillType() == null) {
            bill.setBillType(FactoryBill.BILL_TYPE_FACTORY);
        }

        // 1. 生成账单编号（客户账单用 CB 前缀，工厂账单用 FB 前缀）
        String prefix = bill.isCustomerBill() ? "CB" : "FB";
        String billNo = prefix + System.currentTimeMillis();
        bill.setBillNo(billNo);
        bill.setCreateTime(LocalDateTime.now());
        bill.setUpdateTime(LocalDateTime.now());
        if (bill.getStatus() == null) bill.setStatus(1);

        // 2. 统一月份格式：将 ISO 格式(2026-04) 转为中文格式(2026年04月)
        if (bill.getMonth() != null && !bill.getMonth().isEmpty()) {
            String rawMonth = bill.getMonth();
            // 匹配 ISO 格式: yyyy-MM → 转为 中文格式
            if (rawMonth.matches("^\\d{4}-\\d{2}$")) {
                String[] parts = rawMonth.split("-");
                bill.setMonth(parts[0] + "年" + parts[1] + "月");
            }
        }

        // 3. 根据 customerId（crm_customer.id）查询工厂客户信息
        Long customerId = bill.getCustomerId();
        if (customerId != null && bill.getFactoryName() == null) {
            Customer customer = customerMapper.selectById(customerId);
            if (customer != null) {
                bill.setFactoryName(customer.getCustomerName());
            }
        }

        // 4. 根据 salesmanId 查询业务员姓名并填充冗余字段
        Long salesmanId = bill.getSalesmanId();
        if (salesmanId != null && bill.getSalesmanName() == null) {
            FactorySalesman salesman = factorySalesmanMapper.selectById(salesmanId);
            if (salesman != null) {
                bill.setSalesmanName(salesman.getName());
            }
        }

        // 5. 插入数据库
        factoryBillMapper.insert(bill);
        return Result.ok(bill.getId());
    }

    @PutMapping("/bills/{id}")
    @Operation(summary = "更新账单")
    @PreAuthorize("hasAuthority('factory:edit')")
    public Result<Void> updateBill(@PathVariable Long id, @RequestBody FactoryBill bill) {
        bill.setId(id);
        bill.setUpdateTime(LocalDateTime.now());
        factoryBillMapper.updateById(bill);
        return Result.ok();
    }

    @PutMapping("/bills/{id}/status")
    @Operation(summary = "更新账单状态（含对账/结算）")
    @PreAuthorize("hasAuthority('factory:edit')")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        FactoryBill bill = new FactoryBill();
        bill.setId(id);
        bill.setStatus(status);
        bill.setUpdateTime(LocalDateTime.now());
        factoryBillMapper.updateById(bill);
        return Result.ok();
    }

    @DeleteMapping("/bills/{id}")
    @Operation(summary = "删除账单")
    @PreAuthorize("hasAuthority('factory:delete')")
    public Result<Void> deleteBill(@PathVariable Long id) {
        // 使用原生 SQL 做逻辑删除（绕过 MyBatis-Plus updateById 可能忽略 deleted 字段的问题）
        factoryBillMapper.deleteBillLogical(id);
        // 同时逻辑删除关联明细
        factoryBillDetailMapper.logicDeleteByBillId(id);
        return Result.ok();
    }

    // ========== 工厂账单明细（每日登记记录）==========

    @GetMapping("/bills/{id}/details")
    @Operation(summary = "获取账单明细列表（每日登记记录）")
    @PreAuthorize("hasAuthority('factory:list')")
    public Result<List<FactoryBillDetail>> getBillDetails(@PathVariable Long id) {
        List<FactoryBillDetail> details = factoryBillDetailMapper.selectByBillId(id);
        return Result.ok(details);
    }

    @DeleteMapping("/bills/{billId}/details/{detailId}")
    @Operation(summary = "删除账单明细（单条记录）")
    @PreAuthorize("hasAuthority('factory:delete')")
    public Result<Void> deleteBillDetail(@PathVariable Long billId, @PathVariable Long detailId) {
        factoryBillDetailMapper.deleteDetailLogical(detailId);
        // 重新计算账单总金额
        recalcBillTotal(billId);
        return Result.ok();
    }

    @DeleteMapping("/bills/{billId}/details/batch-cleanup")
    @Operation(summary = "批量清理账单所有明细（编辑时先清空再重新添加）")
    @PreAuthorize("hasAuthority('factory:edit')")
    public Result<Void> cleanupBillDetails(@PathVariable Long billId) {
        factoryBillDetailMapper.logicDeleteByBillId(billId);
        return Result.ok();
    }

    @PostMapping("/bills/{id}/details")
    @Operation(summary = "新增账单明细（每日登记，支持多计价模式）")
    @PreAuthorize("hasAuthority('factory:edit')")
    public Result<Long> addBillDetail(@PathVariable Long id, @RequestBody FactoryBillDetail detail) {
        // 关联账单
        detail.setBillId(id);
        // 同步账单编号
        FactoryBill bill = factoryBillMapper.selectById(id);
        if (bill != null) {
            detail.setBillNo(bill.getBillNo());
        }
        detail.setCreateTime(LocalDateTime.now());
        detail.setUpdateTime(LocalDateTime.now());
        // 根据计价模式自动计算小计金额
        detail.setAmount(calcAmount(detail));
        // 根据 salesmanId 回填业务员姓名
        if (detail.getSalesmanId() != null && detail.getSalesmanName() == null) {
            FactorySalesman salesman = factorySalesmanMapper.selectById(detail.getSalesmanId());
            if (salesman != null) {
                detail.setSalesmanName(salesman.getName());
            }
        }
        factoryBillDetailMapper.insert(detail);
        // 更新账单总额（重新汇总）
        recalcBillTotal(id);
        return Result.ok(detail.getId());
    }

    /**
     * 批量新增账单明细
     * 支持一次提交多项登记记录，一个单子有很多项时一起输入后批量保存
     */
    @PostMapping("/bills/{id}/details/batch")
    @Operation(summary = "批量新增账单明细（一次添加多项）")
    @PreAuthorize("hasAuthority('factory:edit')")
    public Result<List<Long>> addBillDetailsBatch(
            @PathVariable Long id,
            @RequestBody List<FactoryBillDetail> details) {
        if (details == null || details.isEmpty()) {
            return Result.fail("明细列表不能为空");
        }
        
        FactoryBill bill = factoryBillMapper.selectById(id);
        if (bill == null) {
            return Result.fail("账单不存在");
        }
        
        List<Long> ids = new java.util.ArrayList<>();
        for (FactoryBillDetail detail : details) {
            detail.setBillId(id);
            detail.setBillNo(bill.getBillNo());
            detail.setCreateTime(LocalDateTime.now());
            detail.setUpdateTime(LocalDateTime.now());
            // 根据计价模式自动计算小计金额
            detail.setAmount(calcAmount(detail));
            // 根据 salesmanId 回填业务员姓名
            if (detail.getSalesmanId() != null && detail.getSalesmanName() == null) {
                FactorySalesman salesman = factorySalesmanMapper.selectById(detail.getSalesmanId());
                if (salesman != null) {
                    detail.setSalesmanName(salesman.getName());
                }
            }
            factoryBillDetailMapper.insert(detail);
            ids.add(detail.getId());
        }
        
        // 批量插入完成后统一重新汇总账单总额
        recalcBillTotal(id);
        return Result.ok(ids);
    }

    @PutMapping("/bills/{id}/details/{detailId}")
    @Operation(summary = "更新账单明细")
    @PreAuthorize("hasAuthority('factory:edit')")
    public Result<Void> updateBillDetail(@PathVariable Long id, @PathVariable Long detailId, @RequestBody FactoryBillDetail detail) {
        detail.setId(detailId);
        detail.setUpdateTime(LocalDateTime.now());
        // 根据计价模式自动重新计算金额
        detail.setAmount(calcAmount(detail));
        factoryBillDetailMapper.updateById(detail);
        recalcBillTotal(id);
        return Result.ok();
    }

    @PutMapping("/bills/{id}/paid")
    @Operation(summary = "更新账单实际付款金额（支持抹零）")
    @PreAuthorize("hasAuthority('factory:edit')")
    public Result<Void> updatePaidAmount(@PathVariable Long id, @RequestParam BigDecimal paidAmount) {
        FactoryBill bill = new FactoryBill();
        bill.setId(id);
        bill.setPaidAmount(paidAmount);
        bill.setUpdateTime(LocalDateTime.now());
        // 如果付款金额 >= 账单总额，自动标记为已结清
        FactoryBill exist = factoryBillMapper.selectById(id);
        if (exist != null && exist.getTotalAmount() != null && paidAmount.compareTo(exist.getTotalAmount()) >= 0) {
            bill.setStatus(4); // 已结清
        }
        factoryBillMapper.updateById(bill);
        return Result.ok();
    }

    /** 重新计算账单总额（汇总所有明细 amount，四舍五入到2位小数） */
    private void recalcBillTotal(Long billId) {
        List<FactoryBillDetail> details = factoryBillDetailMapper.selectByBillId(billId);
        BigDecimal total = details.stream()
                .map(d -> d.getAmount() != null ? d.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        FactoryBill bill = new FactoryBill();
        bill.setId(billId);
        bill.setTotalAmount(total);
        bill.setUpdateTime(LocalDateTime.now());
        factoryBillMapper.updateById(bill);
    }

    /**
     * 根据计价模式自动计算金额（四舍五入到2位小数）
     * MODE_QUANTITY(1): amount = quantity × unitPrice
     * MODE_AREA(2):     amount = length × width × unitPrice (同时算出面积)
     * MODE_FIXED(3):    amount = unitPrice (unitPrice 直接作为总价)
     */
    private BigDecimal calcAmount(FactoryBillDetail detail) {
        int mode = detail.getCalcMode() != null ? detail.getCalcMode() : FactoryBillDetail.MODE_QUANTITY;
        switch (mode) {
            case FactoryBillDetail.MODE_AREA: {
                // 按面积: 长 × 宽 × 平米单价
                BigDecimal len = detail.getLengthVal() != null ? detail.getLengthVal() : BigDecimal.ZERO;
                BigDecimal wid = detail.getWidthVal() != null ? detail.getWidthVal() : BigDecimal.ZERO;
                BigDecimal price = detail.getUnitPrice() != null ? detail.getUnitPrice() : BigDecimal.ZERO;
                BigDecimal area = len.multiply(wid).setScale(3, RoundingMode.HALF_UP); // 面积保留3位精度
                detail.setAreaSq(area); // 回写面积字段
                return area.multiply(price).setScale(2, RoundingMode.HALF_UP);
            }
            case FactoryBillDetail.MODE_FIXED: {
                // 固定价格: unitPrice 即为总价
                return (detail.getUnitPrice() != null ? detail.getUnitPrice() : BigDecimal.ZERO)
                    .setScale(2, RoundingMode.HALF_UP);
            }
            case FactoryBillDetail.MODE_QUANTITY:
            default: {
                // 按数量: 数量 × 单价
                BigDecimal qty = detail.getQuantity() != null ? detail.getQuantity() : BigDecimal.ONE;
                BigDecimal price = detail.getUnitPrice() != null ? detail.getUnitPrice() : BigDecimal.ZERO;
                return qty.multiply(price).setScale(2, RoundingMode.HALF_UP);
            }
        }
    }
}
