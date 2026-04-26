package com.enterprise.ad.module.customer.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.ad.common.Result;
import com.enterprise.ad.module.customer.entity.CustomerTag;
import com.enterprise.ad.module.customer.mapper.CustomerTagMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 客户标签管理
 *
 * 注意：此 Controller 路径为 /customers/tags，
 * 必须在 CustomerController 之前被 Spring 扫描到，
 * 否则 GET /customers/{id} 会先把 "tags" 当 Long 解析。
 * Spring 默认按精确匹配优先，/customers/tags 比 /customers/{id} 更精确，不会冲突。
 */
@RestController
@RequestMapping("/customers/tags")
@RequiredArgsConstructor
@Tag(name = "客户标签")
public class CustomerTagController {

    private final CustomerTagMapper customerTagMapper;

    @GetMapping
    @Operation(summary = "标签列表")
    @PreAuthorize("hasAuthority('customer:list')")
    public Result<List<CustomerTag>> list() {
        List<CustomerTag> tags = customerTagMapper.selectList(
            new LambdaQueryWrapper<CustomerTag>()
                .eq(CustomerTag::getDeleted, 0)
                .orderByAsc(CustomerTag::getSort)
        );
        return Result.ok(tags);
    }

    @PostMapping
    @Operation(summary = "新增标签")
    @PreAuthorize("hasAuthority('customer:create')")
    public Result<Long> create(@RequestBody CustomerTag tag) {
        tag.setCreateTime(LocalDateTime.now());
        tag.setUpdateTime(LocalDateTime.now());
        if (tag.getStatus() == null) tag.setStatus(1);
        customerTagMapper.insert(tag);
        return Result.ok(tag.getId());
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新标签")
    @PreAuthorize("hasAuthority('customer:edit')")
    public Result<Void> update(@PathVariable Long id, @RequestBody CustomerTag tag) {
        tag.setId(id);
        tag.setUpdateTime(LocalDateTime.now());
        customerTagMapper.updateById(tag);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除标签")
    @PreAuthorize("hasAuthority('customer:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        CustomerTag tag = new CustomerTag();
        tag.setId(id);
        tag.setDeleted(1);
        tag.setUpdateTime(LocalDateTime.now());
        customerTagMapper.updateById(tag);
        return Result.ok();
    }
}
