package com.enterprise.ad.module.member.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 会员流水表（充值/消费记录） */
@Data
@TableName("mem_member_transaction")
public class MemberTransaction {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long memberId;
    private String type;            // 类型：recharge充值 consume消费
    private BigDecimal amount;      // 金额（正数）
    private BigDecimal balanceBefore; // 变动前余额
    private BigDecimal balanceAfter;  // 变动后余额
    private Long orderId;           // 关联订单ID（消费时）
    private String remark;           // 备注
    private Long creatorId;
    private LocalDateTime createTime;
    @TableLogic private Integer deleted;

    /** 以下字段不映射数据库，由接口动态填充 */
    @TableField(exist = false)
    private String transactionNo;   // 流水单号
    @TableField(exist = false)
    private String memberName;      // 会员名称
    @TableField(exist = false)
    private String orderNo;         // 关联订单号
    @TableField(exist = false)
    private BigDecimal giftAmount;  // 赠送金额
    @TableField(exist = false)
    private BigDecimal totalAmount; // 到账金额（充值金额+赠送）
    @TableField(exist = false)
    private String paymentMethod;   // 充值方式
    @TableField(exist = false)
    private String operator;        // 操作人
}
