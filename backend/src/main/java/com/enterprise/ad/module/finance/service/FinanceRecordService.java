package com.enterprise.ad.module.finance.service;

import com.enterprise.ad.module.finance.entity.FinanceRecord;
import com.enterprise.ad.module.finance.mapper.FinanceRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 财务记录服务 — 所有 fin_record 写入的集中入口
 * <p>
 * 职责：
 * 1. 统一生成流水号（按业务前缀）
 * 2. 确保 type/category 值一致
 * 3. 抹零不走这里（抹零不产生实际资金流动）
 * 4. 方便后期扩展：短信通知、审计日志、对账
 */
@Service
@RequiredArgsConstructor
public class FinanceRecordService {

    private final FinanceRecordMapper financeRecordMapper;

    // ── 业务前缀 ──
    /** 订单收款 */
    public static final String PREFIX_ORDER = "ORD";
    /** 快速记账 */
    public static final String PREFIX_QUICK = "FIN";
    /** 退款 */
    public static final String PREFIX_REFUND = "REF";
    /** 物料采购 */
    public static final String PREFIX_MATERIAL = "MAT";
    /** 工厂付款 */
    public static final String PREFIX_FACTORY = "FAC";

    // ── 类别常量 ──
    public static final String CAT_ORDER_INCOME = "订单收款";
    public static final String CAT_REFUND = "订单退款";
    public static final String CAT_QUICK_INCOME = "快速记账-收入";
    public static final String CAT_QUICK_EXPENSE = "快速记账-支出";
    public static final String CAT_MATERIAL_PURCHASE = "物料采购";
    public static final String CAT_MEMBER_RECHARGE = "会员充值";
    public static final String CAT_MEMBER_CONSUME = "会员消费";
    public static final String CAT_FACTORY_PAYMENT = "工厂付款";
    public static final String CAT_SQUARE_INCOME = "设计广场收入";
    public static final String CAT_DESIGNER_COMMISSION = "设计师提成";

    /**
     * 生成流水号：前缀 + 时间戳
     */
    public String generateRecordNo(String prefix) {
        return prefix + System.currentTimeMillis();
    }

    /**
     * 创建收入记录
     *
     * @param category     类别（见 CAT_xxx 常量）
     * @param amount       实际金额（必须 > 0）
     * @param relatedId    关联业务ID
     * @param relatedName  关联名称
     * @param paymentMethod 支付方式
     * @param remark       备注
     * @return 创建的 FinanceRecord
     */
    @Transactional
    public FinanceRecord createIncome(String prefix, String category, BigDecimal amount,
                                      Long relatedId, String relatedName,
                                      String paymentMethod, String remark) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return null; // 无实际资金流入，不记录
        }
        FinanceRecord record = buildRecord(prefix, "income", category, amount,
                relatedId, relatedName, paymentMethod, remark);
        financeRecordMapper.insert(record);
        return record;
    }

    /**
     * 创建支出记录
     */
    @Transactional
    public FinanceRecord createExpense(String prefix, String category, BigDecimal amount,
                                       Long relatedId, String relatedName,
                                       String paymentMethod, String remark) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        FinanceRecord record = buildRecord(prefix, "expense", category, amount,
                relatedId, relatedName, paymentMethod, remark);
        financeRecordMapper.insert(record);
        return record;
    }

    /**
     * 创建退款记录（支出方向，标注退款类别）
     */
    @Transactional
    public FinanceRecord createRefund(BigDecimal amount, Long orderId, String orderNo,
                                      String remark) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        FinanceRecord record = buildRecord(PREFIX_REFUND, "expense", CAT_REFUND,
                amount, orderId, orderNo, "", remark);
        financeRecordMapper.insert(record);
        return record;
    }

    // ── 内部构建 ──

    private FinanceRecord buildRecord(String prefix, String type, String category,
                                       BigDecimal amount, Long relatedId, String relatedName,
                                       String paymentMethod, String remark) {
        FinanceRecord record = new FinanceRecord();
        record.setRecordNo(generateRecordNo(prefix));
        record.setType(type);
        record.setCategory(category);
        record.setAmount(amount);
        record.setRelatedId(relatedId);
        record.setRelatedName(relatedName);
        record.setPaymentMethod(paymentMethod != null ? paymentMethod : "");
        record.setRemark(remark != null ? remark : "");
        record.setCreateTime(LocalDateTime.now());
        record.setDeleted(0);
        return record;
    }

    /**
     * 计算订单待收金额 = totalAmount - paidAmount - roundingAmount
     * 抹零是实际豁免的金额，不产生财务流水
     */
    public static BigDecimal calcUnpaid(BigDecimal total, BigDecimal paid, BigDecimal rounding) {
        BigDecimal t = total != null ? total : BigDecimal.ZERO;
        BigDecimal p = paid != null ? paid : BigDecimal.ZERO;
        BigDecimal r = rounding != null ? rounding : BigDecimal.ZERO;
        return t.subtract(p).subtract(r).max(BigDecimal.ZERO);
    }

    /**
     * 根据已付/总额计算支付状态
     */
    public static int calcPaymentStatus(BigDecimal total, BigDecimal paid, BigDecimal rounding) {
        if (total == null || total.compareTo(BigDecimal.ZERO) <= 0) return 1;
        BigDecimal p = paid != null ? paid : BigDecimal.ZERO;
        BigDecimal r = rounding != null ? rounding : BigDecimal.ZERO;
        if (p.add(r).compareTo(total) >= 0) {
            return r.compareTo(BigDecimal.ZERO) > 0 ? 4 : 3; // 抹零结清 or 已付清
        }
        return p.compareTo(BigDecimal.ZERO) > 0 ? 2 : 1; // 部分付 or 未付
    }
}
