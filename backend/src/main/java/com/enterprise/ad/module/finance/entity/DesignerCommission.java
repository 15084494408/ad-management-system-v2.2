package com.enterprise.ad.module.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 设计师提成表 */
@Data
@TableName("fin_designer_commission")
public class DesignerCommission {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;            // 关联订单ID
    private String orderNo;          // 订单编号（冗余）
    private Long designerId;         // 设计师用户ID
    private String designerName;     // 设计师姓名
    private BigDecimal baseAmount;   // 计算基数
    private BigDecimal commissionRate;   // 提成比例（如 5.00 代表 5%）
    private BigDecimal commissionAmount; // 提成金额
    private Integer status;          // 状态：1待结算 2已结算 3已打款
    private LocalDateTime settleTime;    // 结算时间
    private String remark;           // 备注
    private Long creatorId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic private Integer deleted;
}
