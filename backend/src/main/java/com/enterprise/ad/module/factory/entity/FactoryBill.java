package com.enterprise.ad.module.factory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 工厂账单表（关联工厂客户） */
@Data
@TableName("fac_factory_bill")
public class FactoryBill {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String billNo;          // 账单编号
    /** 工厂客户ID（兼容旧字段，新代码优先用 customerId） */
    @Deprecated
    private Long factoryId;         
    /** 工厂客户ID（关联 crm_customer.id where customer_type=2） */
    private Long customerId;
    /** 客户名称/工厂名称（冗余，方便查询显示） */
    private String factoryName;     
    /** 业务员ID（关联 fac_factory_salesman.id） */
    private Long salesmanId;
    /** 业务员姓名（冗余，方便显示） */
    private String salesmanName;
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

    /** 获取实际使用的客户ID（优先返回 customerId，兼容旧的 factoryId） */
    public Long getActualCustomerId() {
        return customerId != null ? customerId : factoryId;
    }
}
