package com.enterprise.ad.module.factory.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.enterprise.ad.common.Result;
import com.enterprise.ad.module.customer.mapper.CustomerMapper;
import com.enterprise.ad.module.customer.entity.Customer;
import com.enterprise.ad.module.factory.entity.FactorySalesman;
import com.enterprise.ad.module.factory.mapper.FactorySalesmanMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工厂业务员管理（CRUD）
 */
@RestController
@RequestMapping("/factory/salesman")
@RequiredArgsConstructor
@Tag(name = "工厂业务员管理")
public class FactorySalesmanController {

    private final FactorySalesmanMapper salesmanMapper;
    private final CustomerMapper customerMapper;

    /**
     * 业务员列表（支持按工厂筛选）
     */
    @GetMapping
    @Operation(summary = "业务员列表")
    @PreAuthorize("hasAuthority('factory:list')")
    public Result<List<FactorySalesman>> list(
            @RequestParam(required = false) Long factoryId,
            @RequestParam(required = false, defaultValue = "1") Integer status) {
        LambdaQueryWrapper<FactorySalesman> qw = Wrappers.<FactorySalesman>lambdaQuery()
                .eq(factoryId != null, FactorySalesman::getFactoryId, factoryId)
                .eq(status != null, FactorySalesman::getStatus, status)
                .orderByAsc(FactorySalesman::getName);
        List<FactorySalesman> list = salesmanMapper.selectList(qw);
        // 手动填充 factoryName（非数据库字段）
        for (FactorySalesman s : list) {
            if (s.getFactoryId() != null && s.getFactoryName() == null) {
                Customer c = customerMapper.selectById(s.getFactoryId());
                if (c != null) {
                    s.setFactoryName(c.getCustomerName());
                }
            }
        }
        return Result.ok(list);
    }

    /**
     * 获取单个业务员详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "业务员详情")
    @PreAuthorize("hasAuthority('factory:list')")
    public Result<FactorySalesman> getById(@PathVariable Long id) {
        return Result.ok(salesmanMapper.selectById(id));
    }

    /**
     * 新增业务员
     */
    @PostMapping
    @Operation(summary = "新增业务员")
    @PreAuthorize("hasAuthority('factory:create')")
    public Result<Long> create(@RequestBody FactorySalesman salesman) {
        // 如果传了 factoryId，查询并填充工厂名称
        if (salesman.getFactoryId() != null && salesman.getFactoryName() == null) {
            Customer c = customerMapper.selectById(salesman.getFactoryId());
            if (c != null) {
                salesman.setFactoryName(c.getCustomerName());
            }
        }
        salesman.setCreateTime(LocalDateTime.now());
        salesman.setUpdateTime(LocalDateTime.now());
        if (salesman.getStatus() == null) salesman.setStatus(1);
        salesmanMapper.insert(salesman);
        return Result.ok(salesman.getId());
    }

    /**
     * 更新业务员
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新业务员")
    @PreAuthorize("hasAuthority('factory:edit')")
    public Result<Void> update(@PathVariable Long id, @RequestBody FactorySalesman salesman) {
        salesman.setId(id);
        // 如果传了 factoryId，同步更新工厂名称
        if (salesman.getFactoryId() != null) {
            Customer c = customerMapper.selectById(salesman.getFactoryId());
            if (c != null) {
                salesman.setFactoryName(c.getCustomerName());
            }
        } else {
            salesman.setFactoryName(null);
        }
        salesman.setUpdateTime(LocalDateTime.now());
        salesmanMapper.updateById(salesman);
        return Result.ok();
    }

    /**
     * 删除业务员（逻辑删除）
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除业务员")
    @PreAuthorize("hasAuthority('factory:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        FactorySalesman s = new FactorySalesman();
        s.setId(id);
        s.setDeleted(1);
        s.setUpdateTime(LocalDateTime.now());
        salesmanMapper.updateById(s);
        return Result.ok();
    }

    /**
     * 按工厂ID获取启用的业务员列表（用于下拉选择）
     */
    @GetMapping("/options")
    @Operation(summary = "获取业务员选项（用于账单选择）")
    @PreAuthorize("hasAuthority('factory:list')")
    public Result<List<FactorySalesman>> options(@RequestParam Long factoryId) {
        List<FactorySalesman> list = salesmanMapper.selectEnabledByFactoryId(factoryId);
        return Result.ok(list != null ? list : List.of());
    }
}
