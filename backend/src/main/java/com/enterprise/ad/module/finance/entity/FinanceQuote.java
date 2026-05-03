package com.enterprise.ad.module.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.FieldFill;

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
    private Long customerId;        // 客户ID
    private String projectName;     // 项目名称
    private BigDecimal totalAmount; // 报价金额（物料合计）
    private BigDecimal discount;    // 折扣百分比
    private BigDecimal finalAmount; // 最终金额（折扣后）
    private BigDecimal taxRate;     // 税率%
    private BigDecimal taxAmount;   // 税额
    private String status;          // 状态：pending/accepted/rejected/expired
    private String validUntil;      // 有效期至
    private String quoteDate;       // 报价日期
    private Long companyId;         // 报价公司ID
    private String remark;          // 备注
    private Long creatorId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
