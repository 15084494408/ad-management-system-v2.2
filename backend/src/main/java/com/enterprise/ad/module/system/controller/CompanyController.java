package com.enterprise.ad.module.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.ad.common.Result;
import com.enterprise.ad.module.system.entity.SysCompany;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import com.enterprise.ad.module.system.service.SysCompanyService;

@RestController
@RequestMapping("/system/companies")
@RequiredArgsConstructor
@Tag(name = "公司管理")
public class CompanyController {

    private final SysCompanyService sysCompanyService;

    @GetMapping
    @Operation(summary = "公司列表")
    @PreAuthorize("hasAuthority('system:config')")
    public Result<List<SysCompany>> list() {
        List<SysCompany> list = sysCompanyService.list(
            new LambdaQueryWrapper<SysCompany>()
                .eq(SysCompany::getDeleted, 0)
                .orderByDesc(SysCompany::getIsDefault)
                .orderByAsc(SysCompany::getId)
        );
        return Result.ok(list);
    }

    @PostMapping
    @Operation(summary = "新建公司")
    @PreAuthorize("hasAuthority('system:config')")
    public Result<Long> create(@RequestBody SysCompany company) {
        if (company.getCompanyName() == null || company.getCompanyName().isBlank()) {
            return Result.fail(400, "公司名称不能为空");
        }
        // 如果设为默认，先取消其他默认
        if (company.getIsDefault() != null && company.getIsDefault() == 1) {
            clearDefault();
        }
        if (company.getStatus() == null) company.setStatus(1);
        company.setCreateTime(LocalDateTime.now());
        company.setDeleted(0);
        sysCompanyService.save(company);
        return Result.ok(company.getId());
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新公司")
    @PreAuthorize("hasAuthority('system:config')")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysCompany company) {
        company.setId(id);
        if (company.getIsDefault() != null && company.getIsDefault() == 1) {
            clearDefault();
        }
        company.setUpdateTime(LocalDateTime.now());
        sysCompanyService.updateById(company);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除公司")
    @PreAuthorize("hasAuthority('system:config')")
    public Result<Void> delete(@PathVariable Long id) {
        sysCompanyService.removeById(id);
        return Result.ok();
    }

    private void clearDefault() {
        SysCompany update = new SysCompany();
        update.setIsDefault(0);
        update.setUpdateTime(LocalDateTime.now());
        sysCompanyService.update(update,
            new LambdaQueryWrapper<SysCompany>()
                .eq(SysCompany::getIsDefault, 1)
                .eq(SysCompany::getDeleted, 0)
        );
    }
}
