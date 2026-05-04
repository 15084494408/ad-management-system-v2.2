package com.enterprise.ad.module.factory.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.enterprise.ad.common.Result;
import com.enterprise.ad.module.customer.mapper.CustomerMapper;
import com.enterprise.ad.module.customer.entity.Customer;
import com.enterprise.ad.module.factory.entity.FactorySalesman;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import com.enterprise.ad.module.factory.service.FactorySalesmanService;
import com.enterprise.ad.module.factory.service.CustomerService;

/**
 * 工厂业务员管理（CRUD）
 */
@RestController
@RequestMapping("/factory/salesman")
@RequiredArgsConstructor
@Tag(name = "工厂业务员管理")
public class FactorySalesmanController {

    private final FactorySalesmanService factorySalesmanService;
    private final CustomerMapper customerMapper;
    private final CustomerService customerService;

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
        List<FactorySalesman> list = factorySalesmanService.list(qw);
        // 手动填充 factoryName（非数据库字段）
        for (FactorySalesman s : list) {
            if (s.getFactoryId() != null && s.getFactoryName() == null) {
                Customer c = customerService.getById(s.getFactoryId());
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
        return Result.ok(factorySalesmanService.getById(id));
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
            Customer c = customerService.getById(salesman.getFactoryId());
            if (c != null) {
                salesman.setFactoryName(c.getCustomerName());
            }
        }
        salesman.setCreateTime(LocalDateTime.now());
        salesman.setUpdateTime(LocalDateTime.now());
        if (salesman.getStatus() == null) salesman.setStatus(1);
        factorySalesmanService.save(salesman);
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
            Customer c = customerService.getById(salesman.getFactoryId());
            if (c != null) {
                salesman.setFactoryName(c.getCustomerName());
            }
        } else {
            salesman.setFactoryName(null);
        }
        salesman.setUpdateTime(LocalDateTime.now());
        factorySalesmanService.updateById(salesman);
        return Result.ok();
    }

    /**
     * 删除业务员（逻辑删除）
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除业务员")
    @PreAuthorize("hasAuthority('factory:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        // ★ 修复：deleteById 在 @TableLogic 下会自动转为逻辑删除
        factorySalesmanService.removeById(id);
        return Result.ok();
    }

    /**
     * 按工厂ID获取启用的业务员列表（用于下拉选择）
     */
    @GetMapping("/options")
    @Operation(summary = "获取业务员选项（用于账单选择）")
    @PreAuthorize("hasAuthority('factory:list')")
    public Result<List<FactorySalesman>> options(@RequestParam Long factoryId) {
        List<FactorySalesman> list = factorySalesmanService.selectEnabledByFactoryId(factoryId);
        return Result.ok(list != null ? list : List.of());
    }
}
