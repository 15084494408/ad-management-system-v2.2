package com.enterprise.ad.module.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 报价记录表
 */
@Data
@TableName("fin_quote")
public class FinanceQuote {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String quoteNo;         // 报价编号
    private String customerName;    // 客户名称
    private String projectName;     // 项目名称
    private BigDecimal totalAmount; // 报价金额
    private BigDecimal discount;    // 折扣百分比
    private BigDecimal finalAmount; // 最终金额
    private String status;          // 状态：pending/accepted/rejected/expired
    private String validUntil;      // 有效期至
    private String remark;          // 备注
    private Long creatorId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
