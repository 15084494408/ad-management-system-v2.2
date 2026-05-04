package com.enterprise.ad.module.system.dict.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.ad.common.PageResult;
import com.enterprise.ad.common.Result;
import com.enterprise.ad.module.system.dict.entity.DataDict;
import com.enterprise.ad.module.system.dict.entity.DataDictItem;
import com.enterprise.ad.module.system.dict.mapper.DataDictMapper;
import com.enterprise.ad.module.system.dict.mapper.DataDictItemMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/system/dict")
@RequiredArgsConstructor
@Tag(name = "系统管理 - 数据字典")
@PreAuthorize("hasAuthority('system:dict')")
public class DataDictController {

    private final DataDictMapper dictMapper;
    private final DataDictItemMapper itemMapper;

    @GetMapping
    @Operation(summary = "字典列表（分页）")
    public Result<PageResult<DataDict>> list(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String code) {
        Page<DataDict> page = new Page<>(current, size);
        LambdaQueryWrapper<DataDict> qw = new LambdaQueryWrapper<DataDict>()
            .eq(DataDict::getDeleted, 0)
            .like(name != null, DataDict::getName, name)
            .like(code != null, DataDict::getCode, code)
            .orderByDesc(DataDict::getCreateTime);
        Page<DataDict> result = dictMapper.selectPage(page, qw);
        
        // 统计每个字典的项数量
        for (DataDict dict : result.getRecords()) {
            long itemCount = itemMapper.selectCount(
                new LambdaQueryWrapper<DataDictItem>()
                    .eq(DataDictItem::getDictId, dict.getId())
                    .eq(DataDictItem::getDeleted, 0)
            );
            dict.setDescription(String.valueOf(itemCount)); // 临时复用description字段存储数量
        }
        
        return Result.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @GetMapping("/all")
    @Operation(summary = "所有字典（下拉框用）")
    public Result<List<DataDict>> listAll() {
        List<DataDict> list = dictMapper.selectList(
            new LambdaQueryWrapper<DataDict>()
                .eq(DataDict::getDeleted, 0)
                .eq(DataDict::getStatus, 1)
                .orderByAsc(DataDict::getSortOrder)
        );
        return Result.ok(list);
    }

    @GetMapping("/{id}/items")
    @Operation(summary = "字典项列表")
    public Result<List<DataDictItem>> getItems(@PathVariable Long id) {
        List<DataDictItem> items = itemMapper.selectList(
            new LambdaQueryWrapper<DataDictItem>()
                .eq(DataDictItem::getDictId, id)
                .eq(DataDictItem::getDeleted, 0)
                .orderByAsc(DataDictItem::getSortOrder)
        );
        return Result.ok(items);
    }

    @PostMapping
    @Operation(summary = "新增字典")
    public Result<Void> create(@RequestBody DataDict dict) {
        dict.setCreateTime(LocalDateTime.now());
        dict.setStatus(1);
        dictMapper.insert(dict);
        return Result.ok();
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新字典")
    public Result<Void> update(@PathVariable Long id, @RequestBody DataDict dict) {
        dict.setId(id);
        dict.setUpdateTime(LocalDateTime.now());
        dictMapper.updateById(dict);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除字典")
    public Result<Void> delete(@PathVariable Long id) {
        // 先删除字典项
        itemMapper.delete(new LambdaQueryWrapper<DataDictItem>().eq(DataDictItem::getDictId, id));
        dictMapper.deleteById(id);
        return Result.ok();
    }

    @PostMapping("/item")
    @Operation(summary = "新增字典项")
    public Result<Void> createItem(@RequestBody DataDictItem item) {
        item.setCreateTime(LocalDateTime.now());
        itemMapper.insert(item);
        return Result.ok();
    }

    @PutMapping("/item/{id}")
    @Operation(summary = "更新字典项")
    public Result<Void> updateItem(@PathVariable Long id, @RequestBody DataDictItem item) {
        item.setId(id);
        itemMapper.updateById(item);
        return Result.ok();
    }

    @DeleteMapping("/item/{id}")
    @Operation(summary = "删除字典项")
    public Result<Void> deleteItem(@PathVariable Long id) {
        itemMapper.deleteById(id);
        return Result.ok();
    }
}
