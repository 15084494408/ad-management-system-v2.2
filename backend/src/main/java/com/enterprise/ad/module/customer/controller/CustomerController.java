package com.enterprise.ad.module.customer.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.ad.common.PageResult;
import com.enterprise.ad.common.Result;
import com.enterprise.ad.module.customer.entity.Customer;
import com.enterprise.ad.module.customer.entity.CustomerLevel;
import com.enterprise.ad.module.customer.mapper.CustomerMapper;
import com.enterprise.ad.module.customer.mapper.CustomerLevelMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
@Tag(name = "客户管理")
public class CustomerController {

    private final CustomerMapper customerMapper;
    private final CustomerLevelMapper customerLevelMapper;

    @GetMapping
    @Operation(summary = "客户列表（分页，支持按客户类型筛选）")
    @PreAuthorize("hasAuthority('customer:list')")
    public Result<PageResult<Customer>> list(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer level,
            @RequestParam(required = false) Integer customerType) {
        Page<Customer> page = new Page<>(current, size);
        LambdaQueryWrapper<Customer> qw = new LambdaQueryWrapper<Customer>()
            .eq(Customer::getDeleted, 0)
            .eq(level != null, Customer::getLevel, level)
            .eq(customerType != null, Customer::getCustomerType, customerType)
            .and(keyword != null, w -> w
                .like(Customer::getCustomerName, keyword)
                .or()
                .like(Customer::getContactPerson, keyword)
                .or()
                .like(Customer::getPhone, keyword)
            )
            .orderByDesc(Customer::getCreateTime);
        Page<Customer> result = customerMapper.selectPage(page, qw);
        return Result.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    /**
     * 工厂客户列表（便捷接口，等同于 ?customerType=2）
     * 用于工厂账单页面的工厂选择下拉框
     */
    @GetMapping("/factories")
    @Operation(summary = "工厂客户列表（用于账单关联）")
    @PreAuthorize("hasAuthority('customer:list')")
    public Result<List<Customer>> listFactoryCustomers() {
        List<Customer> list = customerMapper.selectList(
            new LambdaQueryWrapper<Customer>()
                .eq(Customer::getDeleted, 0)
                .eq(Customer::getCustomerType, Customer.TYPE_FACTORY)
                .orderByAsc(Customer::getCustomerName));
        return Result.ok(list);
    }

    // ── 客户等级（精确路径必须在 /{id} 之前） ──

    @GetMapping("/levels")
    @Operation(summary = "客户等级列表")
    @PreAuthorize("hasAuthority('customer:list')")
    public Result<List<Object>> listLevels() {
        List<Object> list = customerLevelMapper.listWithCustomerCount().stream()
                .map(m -> {
                    java.util.Map<String, Object> map = (java.util.Map<String, Object>) m;
                    CustomerLevel cl = new CustomerLevel();
                    cl.setId(((Number) map.get("id")).longValue());
                    cl.setName((String) map.get("name"));
                    cl.setLevel(((Number) map.get("level")).intValue());
                    cl.setMinAmount(map.get("min_amount") != null ? new java.math.BigDecimal(map.get("min_amount").toString()) : java.math.BigDecimal.ZERO);
                    cl.setDiscount(((Number) map.get("discount")).intValue());
                    cl.setDescription((String) map.get("description"));
                    java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
                    result.put("id", cl.getId());
                    result.put("name", cl.getName());
                    result.put("level", cl.getLevel());
                    result.put("minAmount", cl.getMinAmount());
                    result.put("discount", cl.getDiscount());
                    result.put("description", cl.getDescription());
                    result.put("customerCount", map.get("customerCount") != null ? ((Number) map.get("customerCount")).longValue() : 0L);
                    return (Object) result;
                }).toList();
        return Result.ok(list);
    }

    @PostMapping("/levels")
    @Operation(summary = "新增客户等级")
    @PreAuthorize("hasAuthority('customer:edit')")
    public Result<Long> createLevel(@RequestBody CustomerLevel level) {
        level.setCreateTime(LocalDateTime.now());
        level.setUpdateTime(LocalDateTime.now());
        if (level.getStatus() == null) level.setStatus(1);
        if (level.getSort() == null) level.setSort(0);
        customerLevelMapper.insert(level);
        return Result.ok(level.getId());
    }

    @PutMapping("/levels/{id}")
    @Operation(summary = "更新客户等级")
    @PreAuthorize("hasAuthority('customer:edit')")
    public Result<Void> updateLevel(@PathVariable Long id, @RequestBody CustomerLevel level) {
        level.setId(id);
        level.setUpdateTime(LocalDateTime.now());
        customerLevelMapper.updateById(level);
        return Result.ok();
    }

    // ── 客户跟进记录（stub） ──

    @GetMapping("/follow")
    @Operation(summary = "客户跟进记录列表")
    @PreAuthorize("hasAuthority('customer:list')")
    public Result<PageResult<Object>> listFollow(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(PageResult.of(0, current, size, java.util.Collections.emptyList()));
    }

    // ── 客户 CRUD ──

    @GetMapping("/{id}")
    @Operation(summary = "客户详情")
    @PreAuthorize("hasAuthority('customer:list')")
    public Result<Customer> getById(@PathVariable Long id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null || customer.getDeleted() == 1) {
            return Result.fail("客户不存在");
        }
        return Result.ok(customer);
    }

    @PostMapping
    @Operation(summary = "新建客户（支持普通/工厂类型）")
    @PreAuthorize("hasAuthority('customer:create')")
    public Result<Long> create(@RequestBody Customer customer) {
        // 校验客户名称（必填）
        String customerName = customer.getCustomerName();
        if (customerName == null || customerName.trim().isEmpty()) {
            return Result.fail("客户名称不能为空");
        }
        customer.setCustomerName(customerName.trim());

        // 默认为普通客户
        if (customer.getCustomerType() == null) customer.setCustomerType(Customer.TYPE_NORMAL);
        
        // 如果是工厂客户但没有选工厂类型，提示选择
        if (customer.getCustomerType() == Customer.TYPE_FACTORY 
            && (customer.getFactoryType() == null || customer.getFactoryType().trim().isEmpty())) {
            return Result.fail("工厂客户必须选择工厂类型（印刷/包装/广告制作）");
        }

        customer.setCreateTime(LocalDateTime.now());
        customer.setUpdateTime(LocalDateTime.now());
        if (customer.getStatus() == null) customer.setStatus(1);
        if (customer.getTotalAmount() == null) customer.setTotalAmount(java.math.BigDecimal.ZERO);
        if (customer.getOrderCount() == null) customer.setOrderCount(0);
        customerMapper.insert(customer);
        return Result.ok(customer.getId());
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新客户")
    @PreAuthorize("hasAuthority('customer:edit')")
    public Result<Void> update(@PathVariable Long id, @RequestBody Customer customer) {
        Customer existing = customerMapper.selectById(id);
        if (existing == null || existing.getDeleted() == 1) {
            return Result.fail("客户不存在");
        }
        // 如果请求中包含客户名称，校验非空
        String customerName = customer.getCustomerName();
        if (customerName != null) {
            if (customerName.trim().isEmpty()) {
                return Result.fail("客户名称不能为空");
            }
            customer.setCustomerName(customerName.trim());
        }
        customer.setId(id);
        customer.setUpdateTime(LocalDateTime.now());
        customerMapper.updateById(customer);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除客户")
    @PreAuthorize("hasAuthority('customer:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setDeleted(1);
        customer.setUpdateTime(LocalDateTime.now());
        customerMapper.updateById(customer);
        return Result.ok();
    }
}
