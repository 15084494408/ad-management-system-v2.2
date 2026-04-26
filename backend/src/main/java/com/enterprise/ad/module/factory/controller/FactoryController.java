package com.enterprise.ad.module.factory.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.ad.common.PageResult;
import com.enterprise.ad.common.Result;
import com.enterprise.ad.module.factory.entity.Factory;
import com.enterprise.ad.module.factory.entity.FactoryBill;
import com.enterprise.ad.module.factory.mapper.FactoryMapper;
import com.enterprise.ad.module.factory.mapper.FactoryBillMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/factory")
@RequiredArgsConstructor
@Tag(name = "工厂管理")
public class FactoryController {

    private final FactoryMapper factoryMapper;
    private final FactoryBillMapper factoryBillMapper;

    // ========== 工厂管理 ==========

    @GetMapping("/list")
    @Operation(summary = "工厂列表")
    @PreAuthorize("hasAuthority('factory:list')")
    public Result<List<Factory>> listFactories() {
        return Result.ok(factoryMapper.selectList(
            new LambdaQueryWrapper<Factory>()
                .eq(Factory::getDeleted, 0)
                .orderByAsc(Factory::getFactoryName)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "工厂详情")
    @PreAuthorize("hasAuthority('factory:list')")
    public Result<Factory> getFactory(@PathVariable Long id) {
        return Result.ok(factoryMapper.selectById(id));
    }

    @PostMapping
    @Operation(summary = "新建工厂")
    @PreAuthorize("hasAuthority('factory:create')")
    public Result<Long> createFactory(@RequestBody Factory factory) {
        factory.setCreateTime(LocalDateTime.now());
        factory.setUpdateTime(LocalDateTime.now());
        if (factory.getStatus() == null) factory.setStatus(1);
        factoryMapper.insert(factory);
        return Result.ok(factory.getId());
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新工厂")
    @PreAuthorize("hasAuthority('factory:edit')")
    public Result<Void> updateFactory(@PathVariable Long id, @RequestBody Factory factory) {
        factory.setId(id);
        factory.setUpdateTime(LocalDateTime.now());
        factoryMapper.updateById(factory);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除工厂")
    @PreAuthorize("hasAuthority('factory:delete')")
    public Result<Void> deleteFactory(@PathVariable Long id) {
        Factory f = new Factory();
        f.setId(id);
        f.setDeleted(1);
        f.setUpdateTime(LocalDateTime.now());
        factoryMapper.updateById(f);
        return Result.ok();
    }

    // ========== 工厂账单 ==========

    @GetMapping("/bills")
    @Operation(summary = "工厂账单列表（支持按工厂/月份/状态筛选）")
    @PreAuthorize("hasAuthority('factory:list')")
    public Result<PageResult<FactoryBill>> listBills(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long factoryId,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) Integer status) {
        Page<FactoryBill> page = new Page<>(current, size);
        LambdaQueryWrapper<FactoryBill> qw = new LambdaQueryWrapper<FactoryBill>()
            .eq(factoryId != null, FactoryBill::getFactoryId, factoryId)
            .eq(status != null, FactoryBill::getStatus, status)
            .like(month != null, FactoryBill::getMonth, month)
            .eq(FactoryBill::getDeleted, 0)
            .orderByDesc(FactoryBill::getCreateTime);
        Page<FactoryBill> result = factoryBillMapper.selectPage(page, qw);
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
    @Operation(summary = "新建账单")
    @PreAuthorize("hasAuthority('factory:create')")
    public Result<Long> createBill(@RequestBody FactoryBill bill) {
        String billNo = "FB" + System.currentTimeMillis();
        bill.setBillNo(billNo);
        bill.setCreateTime(LocalDateTime.now());
        bill.setUpdateTime(LocalDateTime.now());
        if (bill.getStatus() == null) bill.setStatus(1);
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
        FactoryBill bill = new FactoryBill();
        bill.setId(id);
        bill.setDeleted(1);
        bill.setUpdateTime(LocalDateTime.now());
        factoryBillMapper.updateById(bill);
        return Result.ok();
    }
}
