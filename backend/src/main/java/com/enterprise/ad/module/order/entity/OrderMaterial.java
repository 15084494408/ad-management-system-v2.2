package com.enterprise.ad.module.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.FieldFill;

/** 订单物料明细 */
@Data
@TableName("ord_order_material")
public class OrderMaterial {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String materialName;     // 物料名称
    private String spec;             // 规格
    private String unit;             // 单位
    private BigDecimal quantity;    // 数量
    private BigDecimal unitPrice;  // 单价
    private BigDecimal amount;      // 小计金额
    private String remark;
    private BigDecimal unitCost;   // 单位成本（管理员填写，财务/超管可见）
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableLogic private Integer deleted;
}
