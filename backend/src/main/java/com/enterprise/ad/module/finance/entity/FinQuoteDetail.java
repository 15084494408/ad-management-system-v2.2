package com.enterprise.ad.module.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 报价物料明细 */
@Data
@TableName("fin_quote_detail")
public class FinQuoteDetail {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long quoteId;          // 关联报价ID
    private String materialName;   // 物料名称
    private String spec;           // 规格
    private String unit;           // 单位
    private BigDecimal quantity;   // 数量
    private BigDecimal unitPrice;  // 单价
    private BigDecimal amount;     // 小计金额
    private String remark;         // 备注
    private Integer isCustom;      // 是否手动物料(0=否,1=是)
    private LocalDateTime createTime;
    @TableLogic
    private Integer deleted;
}
