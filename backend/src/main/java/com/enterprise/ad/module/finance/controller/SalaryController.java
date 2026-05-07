package com.enterprise.ad.module.finance.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.ad.common.PageResult;
import com.enterprise.ad.common.Result;
import com.enterprise.ad.module.finance.entity.FinanceRecord;
import com.enterprise.ad.module.finance.entity.Salary;
import com.enterprise.ad.module.finance.mapper.FinanceRecordMapper;
import com.enterprise.ad.module.finance.mapper.SalaryMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 工资管理
 */
@Slf4j
@RestController("financeSalaryController")
@RequestMapping("/finance/salary")
@RequiredArgsConstructor
@Tag(name = "工资管理")
public class SalaryController {

    private final SalaryMapper salaryMapper;
    private final FinanceRecordMapper financeRecordMapper;

    /** 获取所有员工列表 */
    @GetMapping("/employees")
    @Operation(summary = "员工列表")
    @PreAuthorize("hasAuthority('finance:view')")
    public Result<List<Map<String, Object>>> employees() {
        return Result.ok(salaryMapper.selectAllEmployees());
    }

    /** 查询某员工某月的已结算提成汇总 */
    @GetMapping("/commission")
    @Operation(summary = "提成汇总（查询某员工某月已结算提成总额）")
    @PreAuthorize("hasAuthority('finance:view')")
    public Result<BigDecimal> getCommission(@RequestParam Long employeeId, @RequestParam String month) {
        BigDecimal commission = salaryMapper.sumCommissionByMonth(employeeId, month);
        return Result.ok(commission);
    }

    /** 工资列表（分页） */
    @GetMapping
    @Operation(summary = "工资列表（分页+筛选）")
    @PreAuthorize("hasAuthority('finance:view')")
    public Result<PageResult<Salary>> list(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Integer status) {
        Page<Salary> page = new Page<>(current, size);
        LambdaQueryWrapper<Salary> wrapper = new LambdaQueryWrapper<Salary>()
                .eq(month != null && !month.isBlank(), Salary::getMonth, month)
                .eq(employeeId != null, Salary::getEmployeeId, employeeId)
                .eq(status != null, Salary::getStatus, status)
                .orderByDesc(Salary::getMonth)
                .orderByAsc(Salary::getEmployeeId);
        salaryMapper.selectPage(page, wrapper);
        return Result.ok(PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    /** 创建工资单（含提成自动汇总） */
    @PostMapping
    @Operation(summary = "创建工资单（自动汇总提成）")
    @PreAuthorize("hasAuthority('finance:view')")
    @Transactional
    public Result<Long> create(@RequestBody Salary salary) {
        // 自动汇总该员工该月的已结算提成
        if (salary.getMonth() != null && salary.getEmployeeId() != null) {
            BigDecimal commission = salaryMapper.sumCommissionByMonth(
                    salary.getEmployeeId(), salary.getMonth());
            salary.setCommissionAmount(commission);
        }
        // 计算实发合计 = 基本工资 + 提成 + 奖金 - 扣款
        BigDecimal base = salary.getBaseSalary() != null ? salary.getBaseSalary() : BigDecimal.ZERO;
        BigDecimal comm = salary.getCommissionAmount() != null ? salary.getCommissionAmount() : BigDecimal.ZERO;
        BigDecimal bonus = salary.getBonus() != null ? salary.getBonus() : BigDecimal.ZERO;
        BigDecimal deduct = salary.getDeduction() != null ? salary.getDeduction() : BigDecimal.ZERO;
        salary.setTotalAmount(base.add(comm).add(bonus).subtract(deduct).max(BigDecimal.ZERO));
        if (salary.getStatus() == null) salary.setStatus(1);
        salary.setCreateTime(LocalDateTime.now());
        salary.setUpdateTime(LocalDateTime.now());
        salaryMapper.insert(salary);
        return Result.ok(salary.getId());
    }

    /** 更新工资单 */
    @PutMapping("/{id}")
    @Operation(summary = "更新工资单")
    @PreAuthorize("hasAuthority('finance:view')")
    @Transactional
    public Result<Void> update(@PathVariable Long id, @RequestBody Salary salary) {
        Salary existing = salaryMapper.selectById(id);
        if (existing == null || existing.getDeleted() == 1) {
            return Result.fail("工资记录不存在");
        }
        // 已发放的不能修改
        if (existing.getStatus() == 2) {
            return Result.fail("已发放的工资不能修改");
        }
        salary.setId(id);
        // 重新计算实发合计
        BigDecimal base = salary.getBaseSalary() != null ? salary.getBaseSalary() : existing.getBaseSalary();
        BigDecimal comm = salary.getCommissionAmount() != null ? salary.getCommissionAmount() : existing.getCommissionAmount();
        BigDecimal bonus = salary.getBonus() != null ? salary.getBonus() : existing.getBonus();
        BigDecimal deduct = salary.getDeduction() != null ? salary.getDeduction() : existing.getDeduction();
        salary.setTotalAmount(base.add(comm).add(bonus).subtract(deduct).max(BigDecimal.ZERO));
        salary.setUpdateTime(LocalDateTime.now());
        salaryMapper.updateById(salary);
        return Result.ok();
    }

    /** 删除工资单 */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除工资单")
    @PreAuthorize("hasAuthority('finance:view')")
    public Result<Void> delete(@PathVariable Long id) {
        Salary existing = salaryMapper.selectById(id);
        if (existing == null || existing.getDeleted() == 1) {
            return Result.fail("工资记录不存在");
        }
        if (existing.getStatus() == 2) {
            return Result.fail("已发放的工资不能删除");
        }
        salaryMapper.deleteById(id);
        return Result.ok();
    }

    /** 发放工资（待发放→已发放，并写入财务支出） */
    @PutMapping("/{id}/pay")
    @Operation(summary = "发放工资（写入财务支出）")
    @PreAuthorize("hasAuthority('finance:view')")
    @Transactional
    public Result<Void> pay(@PathVariable Long id) {
        Salary existing = salaryMapper.selectById(id);
        if (existing == null || existing.getDeleted() == 1) {
            return Result.fail("工资记录不存在");
        }
        if (existing.getStatus() == 2) {
            return Result.fail("该工资已发放");
        }
        if (existing.getTotalAmount() == null || existing.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail("实发金额为0，无需发放");
        }

        // 写入财务支出流水
        FinanceRecord finRecord = new FinanceRecord();
        finRecord.setRecordNo("SAL" + System.currentTimeMillis());
        finRecord.setType("expense");
        finRecord.setCategory("工资");
        finRecord.setAmount(existing.getTotalAmount());
        finRecord.setRelatedId(existing.getEmployeeId());
        finRecord.setRelatedName(existing.getEmployeeName());
        finRecord.setRemark("工资发放 | " + existing.getMonth() + " | " + existing.getEmployeeName());
        finRecord.setCreateTime(LocalDateTime.now());
        finRecord.setDeleted(0);
        financeRecordMapper.insert(finRecord);

        // 更新状态
        existing.setStatus(2);
        existing.setUpdateTime(LocalDateTime.now());
        salaryMapper.updateById(existing);

        log.info("工资发放: id={}, employee={}, month={}, amount={}",
                id, existing.getEmployeeName(), existing.getMonth(), existing.getTotalAmount());
        return Result.ok();
    }

    /** 批量发放工资（按月份） */
    @PostMapping("/batch-pay")
    @Operation(summary = "批量发放工资（按月份，所有待发放→已发放）")
    @PreAuthorize("hasAuthority('finance:view')")
    @Transactional
    public Result<Map<String, Object>> batchPay(@RequestParam String month) {
        List<Salary> pendingList = salaryMapper.selectList(
                new LambdaQueryWrapper<Salary>()
                        .eq(Salary::getMonth, month)
                        .eq(Salary::getStatus, 1));

        int count = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        LocalDateTime now = LocalDateTime.now();

        for (Salary s : pendingList) {
            if (s.getTotalAmount() == null || s.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) continue;

            FinanceRecord finRecord = new FinanceRecord();
            finRecord.setRecordNo("SAL" + System.currentTimeMillis() + count);
            finRecord.setType("expense");
            finRecord.setCategory("工资");
            finRecord.setAmount(s.getTotalAmount());
            finRecord.setRelatedId(s.getEmployeeId());
            finRecord.setRelatedName(s.getEmployeeName());
            finRecord.setRemark("工资批量发放 | " + s.getMonth() + " | " + s.getEmployeeName());
            finRecord.setCreateTime(now);
            finRecord.setDeleted(0);
            financeRecordMapper.insert(finRecord);

            s.setStatus(2);
            s.setUpdateTime(now);
            salaryMapper.updateById(s);
            totalAmount = totalAmount.add(s.getTotalAmount());
            count++;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", count);
        result.put("totalAmount", totalAmount);
        return Result.ok(result);
    }

    /** 自动汇总提成（重新计算某条工资单的提成金额） */
    @PostMapping("/{id}/sync-commission")
    @Operation(summary = "同步提成（重新汇总该员工该月的已结算提成）")
    @PreAuthorize("hasAuthority('finance:view')")
    public Result<BigDecimal> syncCommission(@PathVariable Long id) {
        Salary existing = salaryMapper.selectById(id);
        if (existing == null || existing.getDeleted() == 1) {
            return Result.fail("工资记录不存在");
        }
        BigDecimal commission = salaryMapper.sumCommissionByMonth(
                existing.getEmployeeId(), existing.getMonth());
        existing.setCommissionAmount(commission);
        // 重算实发
        BigDecimal base = existing.getBaseSalary() != null ? existing.getBaseSalary() : BigDecimal.ZERO;
        BigDecimal comm = existing.getCommissionAmount() != null ? existing.getCommissionAmount() : BigDecimal.ZERO;
        BigDecimal bonus = existing.getBonus() != null ? existing.getBonus() : BigDecimal.ZERO;
        BigDecimal deduct = existing.getDeduction() != null ? existing.getDeduction() : BigDecimal.ZERO;
        existing.setTotalAmount(base.add(comm).add(bonus).subtract(deduct).max(BigDecimal.ZERO));
        existing.setUpdateTime(LocalDateTime.now());
        salaryMapper.updateById(existing);
        return Result.ok(commission);
    }
}
