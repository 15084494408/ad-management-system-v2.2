package com.enterprise.ad.module.todo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.ad.common.Result;
import com.enterprise.ad.common.dto.StatusRequest;
import com.enterprise.ad.module.todo.entity.TodoItem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.enterprise.ad.module.todo.service.TodoItemService;
@RestController
@RequestMapping("/todo")
@RequiredArgsConstructor
@Tag(name = "待办事项")
public class TodoController {

    private final TodoItemService todoItemService;

    @GetMapping("/list")
    @Operation(summary = "待办列表")
    @PreAuthorize("hasAuthority('order:list')")
    public Result<List<Map<String, Object>>> list(@RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<TodoItem> qw = new LambdaQueryWrapper<TodoItem>()
            .eq(status != null, TodoItem::getStatus, status)
            .eq(TodoItem::getDeleted, 0)
            .orderByDesc(TodoItem::getCreateTime);

        List<TodoItem> items = todoItemService.list(qw);

        // 统计各状态数量
        Map<String, Long> statusCount = new HashMap<>();
        for (int i = 1; i <= 4; i++) {
            final int s = i;  // lambda 需要 effectively final
            long count = items.stream().filter(it -> it.getStatus() != null && it.getStatus() == s).count();
            statusCount.put("status" + i, count);
        }

        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (TodoItem item : items) {
            Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("id", item.getId());
            map.put("customerName", item.getCustomerName());
            map.put("contactPhone", item.getContactPhone());
            map.put("contactPerson", item.getContactPerson());
            map.put("dimensions", item.getDimensions());
            map.put("requirements", item.getRequirements());
            map.put("status", item.getStatus());
            map.put("statusLabel", statusLabel(item.getStatus()));
            map.put("quoteAmount", item.getQuoteAmount());
            map.put("quoteDetail", item.getQuoteDetail());
            map.put("orderId", item.getOrderId());
            map.put("orderNo", item.getOrderNo());
            map.put("priority", item.getPriority());
            map.put("priorityLabel", priorityLabel(item.getPriority()));
            map.put("source", item.getSource());
            map.put("remark", item.getRemark());
            map.put("createTime", item.getCreateTime());
            map.put("updateTime", item.getUpdateTime());
            result.add(map);
        }

        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("items", result);
        response.put("statusCount", statusCount);
        return Result.ok((List<Map<String, Object>>) java.util.Collections.singletonList(response));
    }

    @PostMapping
    @Operation(summary = "新增待办")
    @PreAuthorize("hasAuthority('order:create')")
    public Result<Long> create(@RequestBody TodoItem item) {
        if (item.getStatus() == null) item.setStatus(1);
        if (item.getPriority() == null) item.setPriority(1);
        item.setCreateTime(LocalDateTime.now());
        item.setUpdateTime(LocalDateTime.now());
        item.setDeleted(0);
        todoItemService.save(item);
        return Result.ok(item.getId());
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新待办")
    @PreAuthorize("hasAuthority('order:edit')")
    public Result<Void> update(@PathVariable Long id, @RequestBody TodoItem item) {
        TodoItem existing = todoItemService.getById(id);
        if (existing == null || existing.getDeleted() == 1) {
            return Result.fail("待办不存在");
        }
        item.setId(id);
        item.setUpdateTime(LocalDateTime.now());
        todoItemService.updateById(item);
        return Result.ok();
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "更新状态")
    @PreAuthorize("hasAuthority('order:edit')")
    public Result<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest body) {
        TodoItem item = new TodoItem();
        item.setId(id);
        item.setStatus(body.getStatus());
        item.setUpdateTime(LocalDateTime.now());
        todoItemService.updateById(item);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除待办")
    @PreAuthorize("hasAuthority('order:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        todoItemService.removeById(id);
        return Result.ok();
    }

    private String statusLabel(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 1 -> "💡新收集";
            case 2 -> "🔍分析中";
            case 3 -> "✅待确认";
            case 4 -> "📝已转订单";
            default -> "未知";
        };
    }

    private String priorityLabel(Integer priority) {
        if (priority == null) return "普通";
        return switch (priority) {
            case 2 -> "🔴紧急";
            case 3 -> "🟠加急";
            default -> "普通";
        };
    }
}
