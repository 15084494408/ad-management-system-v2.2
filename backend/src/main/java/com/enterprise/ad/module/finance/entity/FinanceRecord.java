package com.enterprise.ad.module.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 财务流水表 */
@Data
@TableName("fin_record")
public class FinanceRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String recordNo;       // 流水号
    private String type;            // 类型：income收入 expense支出
    private String category;        // 类别：订单收入/充值收入/退款/采购支出/工资/房租
    private BigDecimal amount;     // 金额
    private Long relatedId;         // 关联ID（订单ID/会员ID）
    private String relatedName;    // 关联名称
    private String paymentMethod;  // 支付方式：现金/转账/微信/支付宝
    private String remark;          // 备注
    private Long creatorId;
    private LocalDateTime createTime;
    @TableLogic private Integer deleted;
}
