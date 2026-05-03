package com.enterprise.ad.module.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.FieldFill;

/**
 * 发票记录表
 */
@Data
@TableName("fin_invoice")
public class FinanceInvoice {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String invoiceNo;       // 发票编号
    private String customerName;    // 客户名称
    private String type;            // 类型：special专票/normal普票/receipt收据
    private BigDecimal amount;      // 发票金额
    private BigDecimal taxRate;     // 税率
    private BigDecimal taxAmount;   // 税额
    private String status;          // 状态：completed已开具/pending待开具/cancelled已作废
    private String issueDate;       // 开具日期
    private String remark;          // 备注
    private Long creatorId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
