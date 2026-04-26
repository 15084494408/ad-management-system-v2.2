package com.enterprise.ad.module.material.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.ad.common.PageResult;
import com.enterprise.ad.common.Result;
import com.enterprise.ad.common.exception.BusinessException;
import com.enterprise.ad.module.material.entity.Material;
import com.enterprise.ad.module.material.entity.MaterialCategory;
import com.enterprise.ad.module.material.entity.StockLog;
import com.enterprise.ad.module.material.mapper.MaterialCategoryMapper;
import com.enterprise.ad.module.material.mapper.MaterialMapper;
import com.enterprise.ad.module.material.mapper.StockLogMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/material")
@RequiredArgsConstructor
@Tag(name = "物料管理")
@PreAuthorize("hasAuthority('material:view')")
public class MaterialController {

    private final MaterialMapper materialMapper;
    private final MaterialCategoryMapper categoryMapper;
    private final StockLogMapper stockLogMapper;

    // ========== 物料分类 ==========

    @GetMapping("/category")
    @Operation(summary = "分类列表")
    public Result<List<MaterialCategory>> listCategories() {
        List<MaterialCategory> list = categoryMapper.selectList(
            new LambdaQueryWrapper<MaterialCategory>()
                .eq(MaterialCategory::getDeleted, 0)
                .orderByAsc(MaterialCategory::getSortOrder)
        );
        return Result.ok(list);
    }

    @PostMapping("/category")
    @Operation(summary = "新增分类")
    public Result<Void> createCategory(@RequestBody MaterialCategory category) {
        category.setCreateTime(LocalDateTime.now());
        categoryMapper.insert(category);
        return Result.ok();
    }

    @PutMapping("/category/{id}")
    @Operation(summary = "更新分类")
    public Result<Void> updateCategory(@PathVariable Long id, @RequestBody MaterialCategory category) {
        category.setId(id);
        categoryMapper.updateById(category);
        return Result.ok();
    }

    @DeleteMapping("/category/{id}")
    @Operation(summary = "删除分类")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        // 检查分类下是否还有物料
        long count = materialMapper.selectCount(
            new LambdaQueryWrapper<Material>()
                .eq(Material::getCategoryId, id)
                .eq(Material::getDeleted, 0)
        );
        if (count > 0) {
            return Result.fail(400, "该分类下还有物料，无法删除");
        }
        categoryMapper.deleteById(id);
        return Result.ok();
    }

    // ========== 物料库存 ==========

    @GetMapping
    @Operation(summary = "物料列表（分页）")
    public Result<PageResult<Material>> list(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status) {
        Page<Material> page = new Page<>(current, size);
        LambdaQueryWrapper<Material> qw = new LambdaQueryWrapper<Material>()
            .eq(Material::getDeleted, 0)
            .eq(categoryId != null, Material::getCategoryId, categoryId)
            .eq(status != null, Material::getStatus, status)
            .like(name != null, Material::getName, name)
            .orderByDesc(Material::getCreateTime);
        Page<Material> result = materialMapper.selectPage(page, qw);
        return Result.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @GetMapping("/all")
    @Operation(summary = "所有物料（下拉框用）")
    public Result<List<Material>> listAll() {
        List<Material> list = materialMapper.selectList(
            new LambdaQueryWrapper<Material>()
                .eq(Material::getDeleted, 0)
                .eq(Material::getStatus, 1)
                .orderByAsc(Material::getName)
        );
        return Result.ok(list);
    }

    @GetMapping("/warning")
    @Operation(summary = "库存预警列表")
    public Result<List<Material>> listWarning() {
        List<Material> list = materialMapper.selectList(
            new LambdaQueryWrapper<Material>()
                .eq(Material::getDeleted, 0)
                .eq(Material::getStatus, 1)
                .apply("(stock_quantity <= warning_quantity OR stock_quantity <= min_quantity)")
                .orderByAsc(Material::getName)
        );
        return Result.ok(list);
    }

    @PostMapping
    @Operation(summary = "新增物料")
    public Result<Void> create(@RequestBody Material material) {
        material.setCreateTime(LocalDateTime.now());
        material.setStatus(1);
        if (material.getStockQuantity() == null) material.setStockQuantity(0);
        materialMapper.insert(material);
        return Result.ok();
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新物料")
    public Result<Void> update(@PathVariable Long id, @RequestBody Material material) {
        material.setId(id);
        material.setUpdateTime(LocalDateTime.now());
        materialMapper.updateById(material);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除物料")
    public Result<Void> delete(@PathVariable Long id) {
        Material m = new Material();
        m.setId(id);
        m.setDeleted(1);
        m.setUpdateTime(LocalDateTime.now());
        materialMapper.updateById(m);
        return Result.ok();
    }

    // ========== 库存操作 ==========

    @PostMapping("/stock-in")
    @Operation(summary = "入库")
    @Transactional
    public Result<Void> stockIn(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        // ★ 修复：从 request attribute 获取用户信息（由 JwtAuthenticationFilter 设置）
        Long userId = (Long) request.getAttribute("userId");
        String username = (String) request.getAttribute("username");

        Long materialId = Long.valueOf(params.get("materialId").toString());
        Integer quantity = Integer.valueOf(params.get("quantity").toString());
        if (quantity <= 0) {
            return Result.fail(400, "入库数量必须大于0");
        }

        Material material = materialMapper.selectById(materialId);
        if (material == null || material.getDeleted() == 1) {
            return Result.fail(400, "物料不存在");
        }

        StockLog log = new StockLog();
        log.setMaterialId(materialId);
        log.setMaterialName(material.getName());
        log.setChangeType(1);
        log.setQuantity(quantity);
        log.setBeforeStock(material.getStockQuantity());
        log.setAfterStock(material.getStockQuantity() + quantity);
        log.setUnitPrice(material.getPrice());
        log.setTotalPrice(material.getPrice().multiply(new BigDecimal(quantity)));
        log.setRemark((String) params.get("remark"));
        log.setOperatorName(username != null ? username : "system");
        log.setCreateTime(LocalDateTime.now());
        stockLogMapper.insert(log);

        material.setStockQuantity(material.getStockQuantity() + quantity);
        material.setUpdateTime(LocalDateTime.now());
        materialMapper.updateById(material);
        return Result.ok();
    }

    @PostMapping("/stock-out")
    @Operation(summary = "出库")
    @Transactional
    public Result<Void> stockOut(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        // ★ 修复：从 request attribute 获取用户信息
        String username = (String) request.getAttribute("username");

        Long materialId = Long.valueOf(params.get("materialId").toString());
        Integer quantity = Integer.valueOf(params.get("quantity").toString());
        if (quantity <= 0) {
            return Result.fail(400, "出库数量必须大于0");
        }

        Material material = materialMapper.selectById(materialId);
        if (material == null || material.getDeleted() == 1) {
            return Result.fail(400, "物料不存在");
        }

        if (material.getStockQuantity() < quantity) {
            return Result.fail(400, "库存不足，当前库存：" + material.getStockQuantity());
        }

        StockLog log = new StockLog();
        log.setMaterialId(materialId);
        log.setMaterialName(material.getName());
        log.setChangeType(2);
        log.setQuantity(-quantity);
        log.setBeforeStock(material.getStockQuantity());
        log.setAfterStock(material.getStockQuantity() - quantity);
        log.setUnitPrice(material.getPrice());
        log.setRemark((String) params.get("remark"));
        log.setOperatorName(username != null ? username : "system");
        log.setCreateTime(LocalDateTime.now());
        stockLogMapper.insert(log);

        material.setStockQuantity(material.getStockQuantity() - quantity);
        material.setUpdateTime(LocalDateTime.now());
        materialMapper.updateById(material);
        return Result.ok();
    }

    @GetMapping("/stock-log")
    @Operation(summary = "库存变动记录")
    public Result<PageResult<StockLog>> stockLog(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) Long materialId) {
        Page<StockLog> page = new Page<>(current, size);
        LambdaQueryWrapper<StockLog> qw = new LambdaQueryWrapper<StockLog>()
            .eq(StockLog::getDeleted, 0)
            .eq(materialId != null, StockLog::getMaterialId, materialId)
            .orderByDesc(StockLog::getCreateTime);
        Page<StockLog> result = stockLogMapper.selectPage(page, qw);
        return Result.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    // ========== 统计概览 ==========

    @GetMapping("/overview")
    @Operation(summary = "物料统计概览")
    public Result<Map<String, Object>> overview() {
        long total = materialMapper.selectCount(new LambdaQueryWrapper<Material>().eq(Material::getDeleted, 0));
        long normal = materialMapper.selectCount(new LambdaQueryWrapper<Material>().eq(Material::getDeleted, 0).eq(Material::getStatus, 1));
        long warning = materialMapper.selectCount(
            new LambdaQueryWrapper<Material>()
                .eq(Material::getDeleted, 0)
                .eq(Material::getStatus, 1)
                .apply("stock_quantity <= warning_quantity")
        );
        return Result.ok(Map.of(
            "totalTypes", total,
            "normalCount", normal,
            "warningCount", warning
        ));
    }
}
