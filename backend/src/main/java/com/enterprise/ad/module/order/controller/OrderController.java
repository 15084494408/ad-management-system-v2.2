package com.enterprise.ad.module.order.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.ad.common.PageResult;
import com.enterprise.ad.common.Result;
import com.enterprise.ad.common.dto.PaymentRequest;
import com.enterprise.ad.common.dto.StatusRequest;
import com.enterprise.ad.common.util.DateUtil;
import com.enterprise.ad.module.designer.entity.DesignerCommissionConfig;
import com.enterprise.ad.module.designer.mapper.DesignerCommissionConfigMapper;
import com.enterprise.ad.module.member.entity.Member;
import com.enterprise.ad.module.member.entity.MemberTransaction;
import com.enterprise.ad.module.member.mapper.MemberMapper;
import com.enterprise.ad.module.member.mapper.MemberTransactionMapper;
import com.enterprise.ad.module.order.entity.Order;
import com.enterprise.ad.module.order.entity.OrderMaterial;
import com.enterprise.ad.module.order.mapper.OrderMapper;
import com.enterprise.ad.module.order.mapper.OrderMaterialMapper;
import com.enterprise.ad.module.system.user.entity.SysUser;
import com.enterprise.ad.module.system.user.mapper.SysUserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "订单管理")
public class OrderController {

    private final OrderMapper orderMapper;
    private final OrderMaterialMapper orderMaterialMapper;
    private final MemberMapper memberMapper;
    private final MemberTransactionMapper memberTransactionMapper;
    private final DesignerCommissionConfigMapper commissionMapper;
    private final SysUserMapper userMapper;
    private final StringRedisTemplate redisTemplate;

    // 订单编号前缀
    private static final String ORDER_PREFIX = "DD";

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
        LocalDateTime startDateTime = DateUtil.startOfDay(startDate);
        LocalDateTime endDateTime = DateUtil.endOfDay(endDate);

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

    /**
     * ★ 修复 P2-14: 订单统计改为 SQL 聚合查询，不再全量加载到 Java 内存
     */
    @GetMapping("/statistics")
    @Operation(summary = "订单统计汇总")
    @PreAuthorize("hasAuthority('order:list')")
    public Result<Map<String, Object>> statistics(
            @RequestParam(defaultValue = "month") String period,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        LocalDate today = LocalDate.now();
        LocalDateTime startDateTime;
        LocalDateTime endDateTime;

        if (startDate != null && endDate != null) {
            startDateTime = startDate.atStartOfDay();
            endDateTime = endDate.atTime(23, 59, 59);
        } else {
            LocalDateTime[] range = DateUtil.parsePeriod(period, today);
            startDateTime = range[0];
            endDateTime = range[1];
        }

        // ★ 使用 SQL 聚合查询，不再全量加载
        long totalCount = orderMapper.countByTimeRange(startDateTime, endDateTime);
        long pendingCount = orderMapper.countByStatusAndTimeRange(1, startDateTime, endDateTime);
        long processingCount = orderMapper.countByStatusAndTimeRange(2, startDateTime, endDateTime);
        long completedCount = orderMapper.countByStatusAndTimeRange(3, startDateTime, endDateTime);
        long cancelledCount = orderMapper.countByStatusAndTimeRange(4, startDateTime, endDateTime);
        BigDecimal totalAmount = orderMapper.sumTotalAmount(startDateTime, endDateTime);
        BigDecimal paidAmount = orderMapper.sumPaidAmount(startDateTime, endDateTime);
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

        // ★ 近7天趋势（SQL 聚合）
        LocalDateTime trendStart = today.minusDays(6).atStartOfDay();
        LocalDateTime trendEnd = today.atTime(23, 59, 59);
        List<Map<String, Object>> dayStats = orderMapper.countByDay(trendStart, trendEnd);
        Map<String, Long> dayMap = new HashMap<>();
        for (Map<String, Object> row : dayStats) {
            String dateStr = row.get("date").toString();
            dayMap.put(dateStr, ((Number) row.get("count")).longValue());
        }

        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", d.toString());
            item.put("count", dayMap.getOrDefault(d.toString(), 0L));
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

        // 计算利润
        BigDecimal totalAmount = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal totalCost = order.getTotalCost() != null ? order.getTotalCost() : BigDecimal.ZERO;
        BigDecimal commission = order.getDesignerCommission() != null ? order.getDesignerCommission() : BigDecimal.ZERO;
        BigDecimal profit = totalAmount.subtract(totalCost).subtract(commission);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("order", order);
        detail.put("materials", materials);
        detail.put("materialTotal", materials.stream()
            .map(m -> m.getQuantity() != null && m.getUnitPrice() != null
                ? m.getQuantity().multiply(m.getUnitPrice())
                : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add));
        // 成本相关（管理员/财务可见）
        detail.put("totalCost", totalCost);
        detail.put("designerCommission", commission);
        detail.put("profit", profit);
        return Result.ok(detail);
    }

    @PostMapping
    @Operation(summary = "新建订单")
    @PreAuthorize("hasAuthority('order:create')")
    @Transactional
    public Result<Long> create(@RequestBody Order order) {
        String orderNo = generateOrderNo();
        order.setOrderNo(orderNo);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        if (order.getStatus() == null) order.setStatus(1);
        if (order.getPaymentStatus() == null) order.setPaymentStatus(1);
        if (order.getPaidAmount() == null) order.setPaidAmount(BigDecimal.ZERO);
        if (order.getDiscountAmount() == null) order.setDiscountAmount(BigDecimal.ZERO);
        if (order.getRoundingAmount() == null) order.setRoundingAmount(BigDecimal.ZERO);
        if (order.getTotalAmount() == null) order.setTotalAmount(BigDecimal.ZERO);
        if (order.getQuoteAmount() == null) order.setQuoteAmount(BigDecimal.ZERO);
        if (order.getDepositAmount() == null) order.setDepositAmount(BigDecimal.ZERO);
        if (order.getPriority() == null) order.setPriority(1);
        if (order.getSource() == null) order.setSource(1);
        if (order.getTotalCost() == null) order.setTotalCost(BigDecimal.ZERO);
        if (order.getDesignerCommission() == null) order.setDesignerCommission(BigDecimal.ZERO);
        orderMapper.insert(order);

        // 同时插入物料明细（预设物料自动带入成本价）
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
            // 重算总额、成本、提成
            recalcOrderAmountAndCost(order.getId());
        }

        // 自动计算设计师提成
        if (order.getDesignerId() != null && order.getTotalAmount() != null) {
            autoCalcCommission(order.getId(), order.getDesignerId(), order.getTotalAmount());
        }

        return Result.ok(order.getId());
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新订单")
    @PreAuthorize("hasAuthority('order:edit')")
    @Transactional
    public Result<Void> update(@PathVariable Long id, @RequestBody Order order) {
        Order existing = orderMapper.selectById(id);
        if (existing == null || existing.getDeleted() == 1) {
            return Result.fail("订单不存在");
        }
        order.setId(id);
        order.setOrderNo(null);  // 不允许修改订单编号
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);

        // 如果订单金额变化，重新计算设计师提成
        if (order.getTotalAmount() != null && !order.getTotalAmount().equals(existing.getTotalAmount())) {
            Long designerId = order.getDesignerId() != null ? order.getDesignerId() : existing.getDesignerId();
            if (designerId != null) {
                autoCalcCommission(id, designerId, order.getTotalAmount());
            }
        } else if (order.getDesignerId() != null && !order.getDesignerId().equals(existing.getDesignerId())) {
            // 设计师变更，重新计算提成
            autoCalcCommission(id, order.getDesignerId(), existing.getTotalAmount() != null ? existing.getTotalAmount() : BigDecimal.ZERO);
        }
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除订单")
    @PreAuthorize("hasAuthority('order:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        orderMapper.deleteById(id);
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

        // 自动更新订单总额和总成本
        recalcOrderAmountAndCost(id);
        return Result.ok();
    }

    /**
     * 更新物料明细（管理员可更新成本）
     */
    @PutMapping("/{id}/materials/{materialId}")
    @Operation(summary = "更新物料明细")
    @PreAuthorize("hasAuthority('order:edit')")
    public Result<Void> updateMaterial(@PathVariable Long id, @PathVariable Long materialId, @RequestBody OrderMaterial material) {
        OrderMaterial existing = orderMaterialMapper.selectById(materialId);
        if (existing == null || existing.getDeleted() == 1 || !existing.getOrderId().equals(id)) {
            return Result.fail("物料不存在");
        }
        material.setId(materialId);
        material.setOrderId(id);
        material.setCreateTime(null); // 不更新创建时间
        orderMaterialMapper.updateById(material);

        // 重新计算订单总额和总成本
        recalcOrderAmountAndCost(id);
        return Result.ok();
    }

    @DeleteMapping("/{id}/materials/{materialId}")
    @Operation(summary = "删除物料明细")
    @PreAuthorize("hasAuthority('order:edit')")
    @Transactional
    public Result<Void> removeMaterial(@PathVariable Long id, @PathVariable Long materialId) {
        orderMaterialMapper.deleteById(materialId);
        recalcOrderAmountAndCost(id);
        return Result.ok();
    }

    /**
     * 更新订单状态
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "更新订单状态")
    @PreAuthorize("hasAuthority('order:edit')")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest body) {
        Integer status = body.getStatus();
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
     * 登记收款 — 支持抹零结清，优先从会员预存金额扣除
     * ★ 修复 P1-7: 提取公共方法 deductMemberBalance()，消除重复代码；使用数据库原子操作保证并发安全
     */
    @PostMapping("/{id}/payment")
    @Operation(summary = "登记收款")
    @PreAuthorize("hasAuthority('order:edit')")
    @Transactional
    public Result<Void> addPayment(@PathVariable Long id, @Valid @RequestBody PaymentRequest paymentReq) {
        Order existing = orderMapper.selectById(id);
        if (existing == null || existing.getDeleted() == 1) {
            return Result.fail("订单不存在");
        }

        BigDecimal amount = paymentReq.getAmount();
        boolean writeOff = Boolean.TRUE.equals(paymentReq.getWriteOff());

        BigDecimal currentPaid = existing.getPaidAmount() != null ? existing.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal total = existing.getTotalAmount() != null ? existing.getTotalAmount() : BigDecimal.ZERO;

        // ★ 提取公共方法：扣除会员预存余额
        deductMemberBalance(existing.getMemberId(), amount, id, existing.getOrderNo());

        // 更新订单已付金额
        BigDecimal newPaid = currentPaid.add(amount);
        Order update = new Order();
        update.setId(id);
        update.setPaidAmount(newPaid);
        update.setUpdateTime(LocalDateTime.now());

        if (writeOff) {
            // 抹零金额 = 订单总额 - 已付金额 - 本次收款金额
            BigDecimal actualRounding = total.subtract(currentPaid).subtract(amount);
            if (actualRounding.compareTo(BigDecimal.ZERO) < 0) {
                actualRounding = BigDecimal.ZERO;
            }
            update.setRoundingAmount(actualRounding);
            update.setPaymentStatus(4); // 4=已抹零结清
        } else {
            // 普通收款：计算实际应收（总额 - 抹零）
            BigDecimal existingRounding = existing.getRoundingAmount() != null ? existing.getRoundingAmount() : BigDecimal.ZERO;
            BigDecimal actualTotal = total.add(existingRounding);

            // 如果已付>=实际应收，自动更新为已付清
            if (newPaid.compareTo(actualTotal) >= 0) {
                update.setPaymentStatus(3);
            } else if (newPaid.compareTo(BigDecimal.ZERO) > 0) {
                update.setPaymentStatus(2);
            }
        }

        orderMapper.updateById(update);
        return Result.ok();
    }

    /**
     * ★ 修复 P1-6: 使用 Redis INCR 原子自增生成订单编号
     * 替代原来的 JVM 级 ReentrantLock（多实例部署下会失效）
     */
    private String generateOrderNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String redisKey = "order:no:seq:" + dateStr;
        String prefix = ORDER_PREFIX + dateStr;

        // Redis INCR 是原子操作，天然支持分布式
        Long seq = redisTemplate.opsForValue().increment(redisKey);
        if (seq != null && seq == 1) {
            // 首次使用，设置 24 小时过期（当天有效）
            redisTemplate.expire(redisKey, 24, TimeUnit.HOURS);
        }

        if (seq == null) {
            // Redis 不可用时的降级方案
            log.warn("Redis 不可用，降级使用数据库查询生成订单号");
            return generateOrderNoFallback(prefix);
        }

        return prefix + String.format("%03d", seq);
    }

    /**
     * Redis 不可用时的降级方案（保留原有逻辑作为兜底）
     */
    private String generateOrderNoFallback(String prefix) {
        String maxNo = orderMapper.selectMaxOrderNo(prefix);
        int seq = 1;
        if (maxNo != null && maxNo.length() > prefix.length()) {
            try {
                seq = Integer.parseInt(maxNo.substring(prefix.length())) + 1;
            } catch (NumberFormatException ignored) {
            }
        }
        return prefix + String.format("%03d", seq);
    }

    /**
     * ★ 修复 P1-7: 提取公共方法 — 扣除会员预存余额
     * 使用数据库原子操作 UPDATE ... WHERE balance >= #{amount} 保证并发安全
     */
    private void deductMemberBalance(Long memberId, BigDecimal amount, Long orderId, String orderNo) {
        if (memberId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        Member member = memberMapper.selectById(memberId);
        if (member == null || member.getBalance() == null || member.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal balance = member.getBalance();
        BigDecimal deductAmount = balance.compareTo(amount) >= 0 ? amount : balance;

        if (deductAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        // ★ 使用原子 UPDATE 语句，避免并发问题
        // SQL: UPDATE mem_member SET balance = balance - #{amount} WHERE balance >= #{amount}
        int rows = memberMapper.deductBalance(memberId, deductAmount);
        if (rows == 0) {
            // 余额不足（可能被其他事务扣减了）
            log.warn("会员 {} 余额扣除失败，余额可能已被其他操作扣减", memberId);
            return;
        }

        // 查询更新后的余额用于记录流水
        Member updatedMember = memberMapper.selectById(memberId);
        BigDecimal newBalance = updatedMember != null && updatedMember.getBalance() != null
            ? updatedMember.getBalance() : BigDecimal.ZERO;

        // 记录流水
        MemberTransaction tx = new MemberTransaction();
        tx.setMemberId(memberId);
        tx.setType("consume");
        tx.setAmount(deductAmount);
        tx.setBalanceBefore(balance);
        tx.setBalanceAfter(newBalance);
        tx.setOrderId(orderId);
        tx.setRemark("订单收款扣除预存：" + orderNo);
        tx.setCreateTime(LocalDateTime.now());
        memberTransactionMapper.insert(tx);
    }

    /**
     * 重算订单金额和成本
     */
    private void recalcOrderAmountAndCost(Long orderId) {
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

        // 计算总成本（单位成本 × 数量）
        BigDecimal totalCost = materials.stream()
            .map(m -> m.getUnitCost() != null && m.getQuantity() != null
                ? m.getUnitCost().multiply(m.getQuantity())
                : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order update = new Order();
        update.setId(orderId);
        update.setTotalAmount(total);
        update.setTotalCost(totalCost);
        update.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(update);
    }

    /**
     * 自动计算设计师提成
     * 提成 = 订单总额 × 设计师提成比例
     */
    private void autoCalcCommission(Long orderId, Long designerId, BigDecimal totalAmount) {
        if (designerId == null || totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        DesignerCommissionConfig config = commissionMapper.selectOne(
            new LambdaQueryWrapper<DesignerCommissionConfig>()
                .eq(DesignerCommissionConfig::getDesignerId, designerId)
                .eq(DesignerCommissionConfig::getEnabled, 1)
                .eq(DesignerCommissionConfig::getDeleted, 0)
        );

        BigDecimal rate = BigDecimal.ZERO;
        if (config != null && config.getCommissionRate() != null && config.getCommissionRate().compareTo(BigDecimal.ZERO) > 0) {
            rate = config.getCommissionRate();
        }

        // 提成 = 总额 × 比例 / 100
        BigDecimal commission = totalAmount.multiply(rate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        // 获取设计师姓名
        SysUser designer = userMapper.selectById(designerId);
        String designerName = designer != null && designer.getRealName() != null
            ? designer.getRealName() : (designer != null ? designer.getUsername() : "");

        Order update = new Order();
        update.setId(orderId);
        update.setDesignerCommission(commission);
        update.setDesignerName(designerName);
        update.setDesignerId(designerId);
        update.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(update);
    }
}
