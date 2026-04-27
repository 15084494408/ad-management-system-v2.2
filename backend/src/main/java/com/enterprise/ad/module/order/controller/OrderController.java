package com.enterprise.ad.module.order.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.ad.common.PageResult;
import com.enterprise.ad.common.Result;
import com.enterprise.ad.module.member.entity.Member;
import com.enterprise.ad.module.member.entity.MemberTransaction;
import com.enterprise.ad.module.member.mapper.MemberMapper;
import com.enterprise.ad.module.member.mapper.MemberTransactionMapper;
import com.enterprise.ad.module.order.entity.Order;
import com.enterprise.ad.module.order.entity.OrderMaterial;
import com.enterprise.ad.module.order.mapper.OrderMapper;
import com.enterprise.ad.module.order.mapper.OrderMaterialMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "订单管理")
public class OrderController {

    private final OrderMapper orderMapper;
    private final OrderMaterialMapper orderMaterialMapper;
    private final MemberMapper memberMapper;
    private final MemberTransactionMapper memberTransactionMapper;

    // 订单编号序列（生产环境应使用 Redis 自增或数据库序列）
    private static final AtomicInteger orderSeq = new AtomicInteger(0);

    @GetMapping
    @Operation(summary = "订单列表（分页+条件筛选）")
    @PreAuthorize("hasAuthority('order:list')")
    public Result<PageResult<Order>> list(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer paymentStatus,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        Page<Order> page = new Page<>(current, size);
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(23, 59, 59) : null;

        String searchKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;

        LambdaQueryWrapper<Order> qw = new LambdaQueryWrapper<Order>()
            .and(searchKeyword != null, w -> w
                .like(Order::getOrderNo, searchKeyword)
                .or().like(Order::getCustomerName, searchKeyword)
                .or().like(Order::getTitle, searchKeyword))
            .like(orderNo != null, Order::getOrderNo, orderNo)
            .like(customerName != null, Order::getCustomerName, customerName)
            .eq(customerId != null, Order::getCustomerId, customerId)
            .eq(status != null, Order::getStatus, status)
            .eq(paymentStatus != null, Order::getPaymentStatus, paymentStatus)
            .ge(startDateTime != null, Order::getCreateTime, startDateTime)
            .le(endDateTime != null, Order::getCreateTime, endDateTime)
            .eq(Order::getDeleted, 0)
            .orderByDesc(Order::getCreateTime);
        Page<Order> result = orderMapper.selectPage(page, qw);
        return Result.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @GetMapping("/statistics")
    @Operation(summary = "订单统计汇总")
    @PreAuthorize("hasAuthority('order:list')")
    public Result<Map<String, Object>> statistics(
            @RequestParam(defaultValue = "month") String period,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        LocalDateTime startDateTime;
        LocalDateTime endDateTime;

        LocalDate today = LocalDate.now();
        if (startDate != null && endDate != null) {
            startDateTime = startDate.atStartOfDay();
            endDateTime = endDate.atTime(23, 59, 59);
        } else {
            switch (period) {
                case "today":
                    startDateTime = today.atStartOfDay();
                    endDateTime = today.atTime(23, 59, 59);
                    break;
                case "week":
                    startDateTime = today.minusDays(today.getDayOfWeek().getValue() - 1).atStartOfDay();
                    endDateTime = today.atTime(23, 59, 59);
                    break;
                case "year":
                    startDateTime = LocalDate.of(today.getYear(), 1, 1).atStartOfDay();
                    endDateTime = today.atTime(23, 59, 59);
                    break;
                default: // month
                    startDateTime = LocalDate.of(today.getYear(), today.getMonth(), 1).atStartOfDay();
                    endDateTime = today.atTime(23, 59, 59);
            }
        }

        LambdaQueryWrapper<Order> qw = new LambdaQueryWrapper<Order>()
            .ge(Order::getCreateTime, startDateTime)
            .le(Order::getCreateTime, endDateTime)
            .eq(Order::getDeleted, 0);

        List<Order> orders = orderMapper.selectList(qw);

        long totalCount = orders.size();
        long completedCount = orders.stream().filter(o -> o.getStatus() != null && o.getStatus() == 3).count();
        long processingCount = orders.stream().filter(o -> o.getStatus() != null && o.getStatus() == 2).count();
        long pendingCount = orders.stream().filter(o -> o.getStatus() != null && o.getStatus() == 1).count();
        long cancelledCount = orders.stream().filter(o -> o.getStatus() != null && o.getStatus() == 4).count();
        BigDecimal totalAmount = orders.stream()
            .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paidAmount = orders.stream()
            .map(o -> o.getPaidAmount() != null ? o.getPaidAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        int completionRate = totalCount > 0 ? (int) (completedCount * 100 / totalCount) : 0;

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalCount", totalCount);
        stats.put("completedCount", completedCount);
        stats.put("processingCount", processingCount);
        stats.put("pendingCount", pendingCount);
        stats.put("cancelledCount", cancelledCount);
        stats.put("totalAmount", totalAmount);
        stats.put("paidAmount", paidAmount);
        stats.put("completionRate", completionRate);

        // 状态分布
        Map<String, Long> statusDistribution = new LinkedHashMap<>();
        statusDistribution.put("pending", pendingCount);
        statusDistribution.put("designing", processingCount);
        statusDistribution.put("completed", completedCount);
        statusDistribution.put("cancelled", cancelledCount);
        stats.put("statusDistribution", statusDistribution);

        // 近7天趋势
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            LocalDateTime dayStart = d.atStartOfDay();
            LocalDateTime dayEnd = d.atTime(23, 59, 59);
            long dayCount = orders.stream()
                .filter(o -> o.getCreateTime() != null && !o.getCreateTime().isBefore(dayStart) && !o.getCreateTime().isAfter(dayEnd))
                .count();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", d.toString());
            item.put("count", dayCount);
            trend.add(item);
        }
        stats.put("trend", trend);

        return Result.ok(stats);
    }

    @GetMapping("/{id}")
    @Operation(summary = "订单详情")
    @PreAuthorize("hasAuthority('order:list')")
    public Result<Map<String, Object>> getById(@PathVariable Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null || order.getDeleted() == 1) {
            return Result.fail("订单不存在");
        }
        // 查询物料明细
        List<OrderMaterial> materials = orderMaterialMapper.selectList(
            new LambdaQueryWrapper<OrderMaterial>()
                .eq(OrderMaterial::getOrderId, id)
                .eq(OrderMaterial::getDeleted, 0)
                .orderByAsc(OrderMaterial::getCreateTime));

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("order", order);
        detail.put("materials", materials);
        detail.put("materialTotal", materials.stream()
            .map(m -> m.getQuantity() != null && m.getUnitPrice() != null
                ? m.getQuantity().multiply(m.getUnitPrice())
                : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add));
        return Result.ok(detail);
    }

    @PostMapping
    @Operation(summary = "新建订单")
    @PreAuthorize("hasAuthority('order:create')")
    @Transactional
    public Result<Long> create(@RequestBody Order order) {
        // ★ 修复：使用更可靠的订单编号生成策略
        String orderNo = generateOrderNo();
        order.setOrderNo(orderNo);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        if (order.getStatus() == null) order.setStatus(1);
        if (order.getPaymentStatus() == null) order.setPaymentStatus(1);
        if (order.getPaidAmount() == null) order.setPaidAmount(BigDecimal.ZERO);
        if (order.getDiscountAmount() == null) order.setDiscountAmount(BigDecimal.ZERO);
        if (order.getTotalAmount() == null) order.setTotalAmount(BigDecimal.ZERO);
        if (order.getQuoteAmount() == null) order.setQuoteAmount(BigDecimal.ZERO);
        if (order.getDepositAmount() == null) order.setDepositAmount(BigDecimal.ZERO);
        if (order.getPriority() == null) order.setPriority(1);
        if (order.getSource() == null) order.setSource(1);
        orderMapper.insert(order);

        // 同时插入物料明细
        if (order.getMaterials() != null) {
            for (OrderMaterial m : order.getMaterials()) {
                m.setOrderId(order.getId());
                m.setCreateTime(LocalDateTime.now());
                m.setDeleted(0);
                if (m.getAmount() == null && m.getQuantity() != null && m.getUnitPrice() != null) {
                    m.setAmount(m.getQuantity().multiply(m.getUnitPrice()));
                }
                orderMaterialMapper.insert(m);
            }
            // 重算总额
            recalcOrderAmount(order.getId());
        }

        return Result.ok(order.getId());
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新订单")
    @PreAuthorize("hasAuthority('order:edit')")
    public Result<Void> update(@PathVariable Long id, @RequestBody Order order) {
        Order existing = orderMapper.selectById(id);
        if (existing == null || existing.getDeleted() == 1) {
            return Result.fail("订单不存在");
        }
        order.setId(id);
        order.setOrderNo(null);  // 不允许修改订单编号
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除订单")
    @PreAuthorize("hasAuthority('order:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        Order order = new Order();
        order.setId(id);
        order.setDeleted(1);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        return Result.ok();
    }

    @GetMapping("/{id}/materials")
    @Operation(summary = "物料明细列表")
    @PreAuthorize("hasAuthority('order:list')")
    public Result<?> getMaterials(@PathVariable Long id) {
        return Result.ok(orderMaterialMapper.selectList(
            new LambdaQueryWrapper<OrderMaterial>()
                .eq(OrderMaterial::getOrderId, id)
                .eq(OrderMaterial::getDeleted, 0)
                .orderByAsc(OrderMaterial::getCreateTime)));
    }

    @PostMapping("/{id}/materials")
    @Operation(summary = "添加物料明细")
    @PreAuthorize("hasAuthority('order:edit')")
    public Result<Void> addMaterial(@PathVariable Long id, @RequestBody OrderMaterial material) {
        material.setOrderId(id);
        material.setCreateTime(LocalDateTime.now());
        material.setDeleted(0);
        if (material.getAmount() == null && material.getQuantity() != null && material.getUnitPrice() != null) {
            material.setAmount(material.getQuantity().multiply(material.getUnitPrice()));
        }
        orderMaterialMapper.insert(material);

        // ★ 新增：自动更新订单总额
        recalcOrderAmount(id);
        return Result.ok();
    }

    @DeleteMapping("/{id}/materials/{materialId}")
    @Operation(summary = "删除物料明细")
    @PreAuthorize("hasAuthority('order:edit')")
    @Transactional
    public Result<Void> removeMaterial(@PathVariable Long id, @PathVariable Long materialId) {
        OrderMaterial m = new OrderMaterial();
        m.setId(materialId);
        m.setDeleted(1);
        orderMaterialMapper.updateById(m);

        recalcOrderAmount(id);
        return Result.ok();
    }

    /**
     * ★ 更新订单状态
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "更新订单状态")
    @PreAuthorize("hasAuthority('order:edit')")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody java.util.Map<String, Integer> body) {
        Integer status = body.get("status");
        if (status == null) {
            return Result.fail(400, "状态不能为空");
        }
        Order order = new Order();
        order.setId(id);
        order.setStatus(status);
        // 状态改为已完成(3)时，同步支付状态为已付清(3)
        if (status == 3) {
            order.setPaymentStatus(3);
        }
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        return Result.ok();
    }

    /**
     * 登记收款 — 优先从会员预存金额扣除，累加已付金额
     */
    @PostMapping("/{id}/payment")
    @Operation(summary = "登记收款")
    @PreAuthorize("hasAuthority('order:edit')")
    @Transactional
    public Result<Void> addPayment(@PathVariable Long id, @RequestBody java.util.Map<String, Object> body) {
        Order existing = orderMapper.selectById(id);
        if (existing == null || existing.getDeleted() == 1) {
            return Result.fail("订单不存在");
        }

        BigDecimal amount = new BigDecimal(body.getOrDefault("amount", "0").toString());
        BigDecimal currentPaid = existing.getPaidAmount() != null ? existing.getPaidAmount() : BigDecimal.ZERO;

        // 优先从会员预存金额扣除
        BigDecimal fromBalance = BigDecimal.ZERO;  // 从预存扣除的金额
        BigDecimal fromOther = amount;                // 其他支付方式金额

        Long memberId = existing.getMemberId();
        if (memberId != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            Member member = memberMapper.selectById(memberId);
            if (member != null && member.getBalance() != null && member.getBalance().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal balance = member.getBalance();
                // 预存金额足够覆盖本次收款
                if (balance.compareTo(amount) >= 0) {
                    fromBalance = amount;
                    fromOther = BigDecimal.ZERO;
                } else {
                    // 预存不够，先扣完预存，剩余用其他方式支付
                    fromBalance = balance;
                    fromOther = amount.subtract(balance);
                }

                // 更新会员余额
                BigDecimal newBalance = balance.subtract(fromBalance);
                Member memberUpdate = new Member();
                memberUpdate.setId(memberId);
                memberUpdate.setBalance(newBalance);
                memberUpdate.setTotalConsume(
                    (member.getTotalConsume() != null ? member.getTotalConsume() : BigDecimal.ZERO)
                        .add(fromBalance)
                );
                memberUpdate.setUpdateTime(LocalDateTime.now());
                memberMapper.updateById(memberUpdate);

                // 记录会员消费流水
                MemberTransaction tx = new MemberTransaction();
                tx.setMemberId(memberId);
                tx.setType("consume");
                tx.setAmount(fromBalance);
                tx.setBalanceBefore(balance);
                tx.setBalanceAfter(newBalance);
                tx.setOrderId(id);
                tx.setRemark("订单收款扣除预存：" + existing.getOrderNo());
                tx.setCreateTime(LocalDateTime.now());
                memberTransactionMapper.insert(tx);
            }
        }

        // 更新订单已付金额（本次收款总额）
        BigDecimal newPaid = currentPaid.add(amount);
        Order update = new Order();
        update.setId(id);
        update.setPaidAmount(newPaid);

        // 如果已付>=总额，自动更新为已付清
        BigDecimal total = existing.getTotalAmount() != null ? existing.getTotalAmount() : BigDecimal.ZERO;
        if (newPaid.compareTo(total) >= 0) {
            update.setPaymentStatus(3);
        } else if (newPaid.compareTo(BigDecimal.ZERO) > 0) {
            update.setPaymentStatus(2);
        }
        update.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(update);

        return Result.ok();
    }

    /**
     * 生成唯一订单编号：DD + 年月日 + 3位序列号
     */
    private String generateOrderNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int seq = orderSeq.incrementAndGet() % 1000;
        return "DD" + dateStr + String.format("%03d", seq);
    }

    /**
     * 重算订单金额
     */
    private void recalcOrderAmount(Long orderId) {
        var materials = orderMaterialMapper.selectList(
            new LambdaQueryWrapper<OrderMaterial>()
                .eq(OrderMaterial::getOrderId, orderId)
                .eq(OrderMaterial::getDeleted, 0)
        );
        BigDecimal total = materials.stream()
            .map(m -> m.getQuantity() != null && m.getUnitPrice() != null
                ? m.getQuantity().multiply(m.getUnitPrice())
                : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order update = new Order();
        update.setId(orderId);
        update.setTotalAmount(total);
        update.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(update);
    }
}
