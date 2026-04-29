package com.enterprise.ad.module.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.enterprise.ad.module.order.service.OrderService;
import com.enterprise.ad.module.system.user.entity.SysUser;
import com.enterprise.ad.module.system.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 订单服务层实现
 * ★ 从 OrderController 抽取的核心业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderMaterialMapper orderMaterialMapper;
    private final MemberMapper memberMapper;
    private final MemberTransactionMapper memberTransactionMapper;
    private final DesignerCommissionConfigMapper commissionMapper;
    private final SysUserMapper userMapper;
    private final StringRedisTemplate redisTemplate;

    private static final String ORDER_PREFIX = "DD";

    @Override
    @Transactional
    public Long createOrder(Order order) {
        String orderNo = generateOrderNo();
        order.setOrderNo(orderNo);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        setDefaultValues(order);
        orderMapper.insert(order);

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
            recalcOrderAmountAndCost(order.getId());
        }

        if (order.getDesignerId() != null && order.getTotalAmount() != null) {
            autoCalcCommission(order.getId(), order.getDesignerId(), order.getTotalAmount());
        }

        return order.getId();
    }

    @Override
    @Transactional
    public void updateOrder(Long id, Order order) {
        Order existing = orderMapper.selectById(id);
        if (existing == null || existing.getDeleted() == 1) {
            throw new IllegalArgumentException("订单不存在");
        }
        order.setId(id);
        order.setOrderNo(null);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);

        if (order.getTotalAmount() != null && !order.getTotalAmount().equals(existing.getTotalAmount())) {
            Long designerId = order.getDesignerId() != null ? order.getDesignerId() : existing.getDesignerId();
            if (designerId != null) {
                autoCalcCommission(id, designerId, order.getTotalAmount());
            }
        } else if (order.getDesignerId() != null && !order.getDesignerId().equals(existing.getDesignerId())) {
            autoCalcCommission(id, order.getDesignerId(), existing.getTotalAmount() != null ? existing.getTotalAmount() : BigDecimal.ZERO);
        }
    }

    @Override
    public void deleteOrder(Long id) {
        orderMapper.deleteById(id);
    }

    @Override
    public Order getOrderDetail(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null || order.getDeleted() == 1) {
            throw new IllegalArgumentException("订单不存在");
        }
        return order;
    }

    @Override
    @Transactional
    public void addMaterial(Long orderId, OrderMaterial material) {
        material.setOrderId(orderId);
        material.setCreateTime(LocalDateTime.now());
        material.setDeleted(0);
        if (material.getAmount() == null && material.getQuantity() != null && material.getUnitPrice() != null) {
            material.setAmount(material.getQuantity().multiply(material.getUnitPrice()));
        }
        orderMaterialMapper.insert(material);
        recalcOrderAmountAndCost(orderId);
    }

    @Override
    @Transactional
    public void updateMaterial(Long orderId, Long materialId, OrderMaterial material) {
        OrderMaterial existing = orderMaterialMapper.selectById(materialId);
        if (existing == null || existing.getDeleted() == 1 || !existing.getOrderId().equals(orderId)) {
            throw new IllegalArgumentException("物料不存在");
        }
        material.setId(materialId);
        material.setOrderId(orderId);
        material.setCreateTime(null);
        orderMaterialMapper.updateById(material);
        recalcOrderAmountAndCost(orderId);
    }

    @Override
    @Transactional
    public void removeMaterial(Long orderId, Long materialId) {
        orderMaterialMapper.deleteById(materialId);
        recalcOrderAmountAndCost(orderId);
    }

    @Override
    @Transactional
    public void addPayment(Long orderId, BigDecimal amount, boolean writeOff) {
        Order existing = orderMapper.selectById(orderId);
        if (existing == null || existing.getDeleted() == 1) {
            throw new IllegalArgumentException("订单不存在");
        }

        deductMemberBalance(existing.getMemberId(), amount, orderId, existing.getOrderNo());

        BigDecimal currentPaid = existing.getPaidAmount() != null ? existing.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal total = existing.getTotalAmount() != null ? existing.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal newPaid = currentPaid.add(amount);

        Order update = new Order();
        update.setId(orderId);
        update.setPaidAmount(newPaid);
        update.setUpdateTime(LocalDateTime.now());

        if (writeOff) {
            BigDecimal actualRounding = total.subtract(currentPaid).subtract(amount);
            if (actualRounding.compareTo(BigDecimal.ZERO) < 0) {
                actualRounding = BigDecimal.ZERO;
            }
            update.setRoundingAmount(actualRounding);
            update.setPaymentStatus(4);
        } else {
            BigDecimal existingRounding = existing.getRoundingAmount() != null ? existing.getRoundingAmount() : BigDecimal.ZERO;
            BigDecimal actualTotal = total.add(existingRounding);
            if (newPaid.compareTo(actualTotal) >= 0) {
                update.setPaymentStatus(3);
            } else if (newPaid.compareTo(BigDecimal.ZERO) > 0) {
                update.setPaymentStatus(2);
            }
        }

        orderMapper.updateById(update);
    }

    // ========== 私有方法 ==========

    private void setDefaultValues(Order order) {
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
    }

    private String generateOrderNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String redisKey = "order:no:seq:" + dateStr;
        String prefix = ORDER_PREFIX + dateStr;

        Long seq = redisTemplate.opsForValue().increment(redisKey);
        if (seq != null && seq == 1) {
            redisTemplate.expire(redisKey, 24, TimeUnit.HOURS);
        }

        if (seq == null) {
            log.warn("Redis 不可用，降级使用数据库查询生成订单号");
            return generateOrderNoFallback(prefix);
        }

        return prefix + String.format("%03d", seq);
    }

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

        int rows = memberMapper.deductBalance(memberId, deductAmount);
        if (rows == 0) {
            log.warn("会员 {} 余额扣除失败，余额可能已被其他操作扣减", memberId);
            return;
        }

        Member updatedMember = memberMapper.selectById(memberId);
        BigDecimal newBalance = updatedMember != null && updatedMember.getBalance() != null
            ? updatedMember.getBalance() : BigDecimal.ZERO;

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

        BigDecimal commission = totalAmount.multiply(rate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

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
