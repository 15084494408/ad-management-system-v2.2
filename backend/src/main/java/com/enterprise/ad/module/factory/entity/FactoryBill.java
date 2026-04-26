package com.enterprise.ad.module.factory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 工厂账单表 */
@Data
@TableName("fac_factory_bill")
public class FactoryBill {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String billNo;          // 账单编号
    private Long factoryId;         // 工厂ID
    private String factoryName;     // 工厂名称（冗余）
    private String month;           // 账单月份（如 2026年04月）
    private BigDecimal totalAmount; // 账单总额
    private BigDecimal paidAmount; // 已付金额
    private Integer status;         // 状态：1未对账 2已对账 3部分付款 4已结清
    private String reconcileFile;  // 对账文件URL
    private String remark;          // 备注
    private Long creatorId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic private Integer deleted;
}
