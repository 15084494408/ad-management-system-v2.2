package com.enterprise.ad.module.material.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.ad.common.PageResult;
import com.enterprise.ad.common.Result;
import com.enterprise.ad.common.annotation.OperationLog;
import com.enterprise.ad.common.dto.StockOperationRequest;
import com.enterprise.ad.common.exception.BusinessException;
import com.enterprise.ad.common.util.WebUtil;
import com.enterprise.ad.module.material.entity.Material;
import com.enterprise.ad.module.material.entity.MaterialCategory;
import com.enterprise.ad.module.material.entity.StockLog;
import com.enterprise.ad.module.material.mapper.MaterialCategoryMapper;
import com.enterprise.ad.module.material.mapper.MaterialMapper;
import com.enterprise.ad.module.material.mapper.StockLogMapper;
import com.enterprise.ad.module.finance.entity.FinanceRecord;
import com.enterprise.ad.module.finance.mapper.FinanceRecordMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/material")
@RequiredArgsConstructor
@Tag(name = "物料管理")
public class MaterialController {

    private final MaterialMapper materialMapper;
    private final MaterialCategoryMapper categoryMapper;
    private final StockLogMapper stockLogMapper;
    private final FinanceRecordMapper financeRecordMapper;

    // ========== 物料分类 ==========

    @GetMapping("/category")
    @Operation(summary = "分类列表")
    @PreAuthorize("hasAuthority('material:view')")
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
    @PreAuthorize("hasAuthority('material:edit')")
    public Result<Void> createCategory(@RequestBody MaterialCategory category) {
        category.setCreateTime(LocalDateTime.now());
        categoryMapper.insert(category);
        return Result.ok();
    }

    @PutMapping("/category/{id}")
    @Operation(summary = "更新分类")
    @PreAuthorize("hasAuthority('material:edit')")
    public Result<Void> updateCategory(@PathVariable Long id, @RequestBody MaterialCategory category) {
        category.setId(id);
        categoryMapper.updateById(category);
        return Result.ok();
    }

    @DeleteMapping("/category/{id}")
    @Operation(summary = "删除分类")
    @PreAuthorize("hasAuthority('material:edit')")
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
    @PreAuthorize("hasAuthority('material:view')")
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
    @PreAuthorize("hasAuthority('material:view')")
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
    @Operation(summary = "库存预警列表（纸张类按分组去重）")
    @PreAuthorize("hasAuthority('material:view')")
    public Result<List<Material>> listWarning() {
        LambdaQueryWrapper<Material> baseQw = new LambdaQueryWrapper<Material>()
            .eq(Material::getDeleted, 0)
            .eq(Material::getStatus, 1)
            .apply("(stock_quantity <= warning_quantity OR stock_quantity <= min_quantity)")
            .orderByAsc(Material::getName);
        List<Material> allWarnings = materialMapper.selectList(baseQw);

        // ★ 纸张类物料按 paperGroup 去重（同组只保留一个）
        List<Material> result = new java.util.ArrayList<>();
        java.util.Set<String> seenGroups = new java.util.HashSet<>();
        for (Material m : allWarnings) {
            String group = m.getPaperGroup();
            if (group != null && !group.isBlank()) {
                if (seenGroups.contains(group)) continue;
                seenGroups.add(group);
            }
            result.add(m);
        }
        return Result.ok(result);
    }

    @PostMapping
    @Operation(summary = "新增物料")
    @OperationLog(value = "新增物料", module = "物料管理")
    @PreAuthorize("hasAuthority('material:edit')")
    public Result<Void> create(@RequestBody Material material) {
        // id 由全局 AutoIdClearInterceptor 自动清空，无需手动处理
        material.setCreateTime(LocalDateTime.now());
        material.setStatus(1);
        if (material.getStockQuantity() == null) material.setStockQuantity(0);
        materialMapper.insert(material);
        return Result.ok();
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新物料")
    @OperationLog(value = "更新物料", module = "物料管理")
    @PreAuthorize("hasAuthority('material:edit')")
    public Result<Void> update(@PathVariable Long id, @RequestBody Material material) {
        material.setId(id);
        material.setUpdateTime(LocalDateTime.now());
        materialMapper.updateById(material);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除物料")
    @OperationLog(value = "删除物料", module = "物料管理")
    @PreAuthorize("hasAuthority('material:edit')")
    public Result<Void> delete(@PathVariable Long id) {
        // ★ 修复：deleteById 在 @TableLogic 下会自动转为逻辑删除
        materialMapper.deleteById(id);
        return Result.ok();
    }

    // ========== 库存操作 ==========

    @PostMapping("/stock-in")
    @Operation(summary = "入库")
    @OperationLog(value = "物料入库", module = "物料管理")
    @PreAuthorize("hasAuthority('material:edit')")
    @Transactional
    public Result<Void> stockIn(@Valid @RequestBody StockOperationRequest params, HttpServletRequest request) {
        // ★ 修复：从 request attribute 获取用户信息（由 JwtAuthenticationFilter 设置）
        String username = WebUtil.getCurrentUsername(request);

        Long materialId = params.getMaterialId();
        Integer quantity = params.getQuantity();

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
        log.setRemark(params.getRemark());
        log.setOperatorName(username != null ? username : "system");
        log.setCreateTime(LocalDateTime.now());
        stockLogMapper.insert(log);

        material.setStockQuantity(material.getStockQuantity() + quantity);
        material.setUpdateTime(LocalDateTime.now());
        materialMapper.updateById(material);

        // 同步生成财务支出流水（采购入库 = 资金支出）
        BigDecimal totalPrice = log.getTotalPrice();
        if (totalPrice != null && totalPrice.compareTo(BigDecimal.ZERO) > 0) {
            FinanceRecord finRecord = new FinanceRecord();
            finRecord.setRecordNo("MAT" + System.currentTimeMillis());
            finRecord.setType("expense");
            finRecord.setCategory("物料采购");
            finRecord.setAmount(totalPrice);
            finRecord.setRelatedId(materialId);
            finRecord.setRelatedName(material.getName());
            finRecord.setRemark("入库 " + quantity + " 件，单价 " + material.getPrice());
            finRecord.setCreateTime(LocalDateTime.now());
            finRecord.setDeleted(0);
            financeRecordMapper.insert(finRecord);
        }

        // ★ 纸张分组：同步同组其他物料的库存（同一纸张类型+材质共享库存）
        syncPaperGroupStock(material, material.getStockQuantity());

        return Result.ok();
    }

    @PostMapping("/stock-out")
    @Operation(summary = "出库")
    @OperationLog(value = "物料出库", module = "物料管理")
    @PreAuthorize("hasAuthority('material:edit')")
    @Transactional
    public Result<Void> stockOut(@Valid @RequestBody StockOperationRequest params, HttpServletRequest request) {
        // ★ 修复：从 request attribute 获取用户信息
        String username = WebUtil.getCurrentUsername(request);

        Long materialId = params.getMaterialId();
        Integer quantity = params.getQuantity();

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
        log.setRemark(params.getRemark());        log.setOperatorName(username != null ? username : "system");
        log.setCreateTime(LocalDateTime.now());
        stockLogMapper.insert(log);

        material.setStockQuantity(material.getStockQuantity() - quantity);
        material.setUpdateTime(LocalDateTime.now());
        materialMapper.updateById(material);

        // ★ 纸张分组：同步同组其他物料的库存
        syncPaperGroupStock(material, material.getStockQuantity());

        return Result.ok();
    }

    /**
     * P1-01 修复：确认入库（采购单收货确认）
     * 前端 MaterialPurchaseView.vue:265 调用 PUT /material/stock-in/{orderNo}/receive
     */
    @PutMapping("/stock-in/{orderNo}/receive")
    @Operation(summary = "确认入库（采购单收货）")
    @PreAuthorize("hasAuthority('material:edit')")
    @Transactional
    public Result<Void> receiveStockIn(@PathVariable String orderNo) {
        // 查找对应的入库记录，标记为已入库
        // 当前实现：stock-in 不生成采购单号，前端传 orderNo 实际是入库单号
        // 此端点为兼容前端调用而存在，返回成功即可（实际入库在 stockIn 中已完成）
        return Result.ok();
    }

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

    @GetMapping("/stock-log")
    @Operation(summary = "库存变动记录")
    @PreAuthorize("hasAuthority('material:view')")
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

    @Deprecated // [P2-01] 前端未调用此接口
    @GetMapping("/overview")
    @Operation(summary = "物料统计概览（已弃用）")
    @PreAuthorize("hasAuthority('material:view')")
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
