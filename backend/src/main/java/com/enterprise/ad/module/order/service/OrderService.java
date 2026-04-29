package com.enterprise.ad.module.order.service;

import com.enterprise.ad.module.order.entity.Order;
import com.enterprise.ad.module.order.entity.OrderMaterial;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单服务层接口
 * ★ 从 OrderController 抽取业务逻辑，解决 Controller 直接操作 Mapper 的架构问题
 */
public interface OrderService {

    /** 创建订单（含物料明细、自动计算设计师提成） */
    Long createOrder(Order order);

    /** 更新订单（金额/设计师变更时自动重算提成） */
    void updateOrder(Long id, Order order);

    /** 删除订单 */
    void deleteOrder(Long id);

    /** 获取订单详情（含物料明细、利润计算） */
    Order getOrderDetail(Long id);

    /** 添加物料明细 */
    void addMaterial(Long orderId, OrderMaterial material);

    /** 更新物料明细 */
    void updateMaterial(Long orderId, Long materialId, OrderMaterial material);

    /** 删除物料明细 */
    void removeMaterial(Long orderId, Long materialId);

    /** 登记收款（支持抹零结清，优先扣除会员预存余额） */
    void addPayment(Long orderId, BigDecimal amount, boolean writeOff);
}
