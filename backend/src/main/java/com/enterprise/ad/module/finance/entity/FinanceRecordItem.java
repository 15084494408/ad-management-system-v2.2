package com.enterprise.ad.module.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.FieldFill;

/** 财务流水物料明细表 */
@Data
@TableName("fin_record_item")
public class FinanceRecordItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long recordId;         // 关联流水ID
    private Long materialId;       // 物料ID
    private String materialName;   // 物料名称
    private Integer pricingType;   // 计价方式：0按数量 1按面积
    private Integer quantity;      // 数量（按数量计价时使用）
    private BigDecimal width;      // 宽度（按面积计价时使用）
    private BigDecimal height;     // 高度（按面积计价时使用）
    private BigDecimal area;       // 面积（宽×高）
    private BigDecimal unitPrice;  // 单价
    private BigDecimal totalPrice; // 小计金额（向上取整）
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableLogic private Integer deleted;
}
