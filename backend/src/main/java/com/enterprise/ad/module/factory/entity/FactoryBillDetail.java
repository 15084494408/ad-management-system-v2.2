package com.enterprise.ad.module.factory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 工厂账单明细（每日登记记录）— 支持多计价模式 */
@Data
@TableName("fac_factory_bill_detail")
public class FactoryBillDetail {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long billId;          // 关联账单ID
    private String billNo;          // 账单编号（冗余）
    private LocalDate recordDate;   // 登记日期
    private String itemName;        // 项目名称
    private String spec;            // 规格说明
    private BigDecimal quantity;     // 数量（按数量计价时使用）
    private String unit;            // 单位（张/本/个/平方米/项）
    private BigDecimal unitPrice;    // 单价（按数量=每件单价，按面积=每㎡单价）
    
    /** 计价方式: 1=按数量 2=按面积(平方) 3=固定价格 */
    private Integer calcMode;
    /** 长度(m)，按面积计价时使用 */
    private BigDecimal lengthVal;
    /** 宽度(m)，按面积计价时使用 */
    private BigDecimal widthVal;
    /** 计算面积(㎡，系统自动算) */
    private BigDecimal areaSq;
    
    private BigDecimal amount;       // 小计金额（根据 calcMode 自动计算）
    
    /** 业务员ID（关联 fac_factory_salesman.id） */
    private Long salesmanId;
    /** 业务员姓名（冗余，方便显示） */
    private String salesmanName;
    
    private String remark;          // 备注
    private Long creatorId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
    
    // ========== 计价模式常量 ==========
    /** 按数量计价: amount = quantity * unitPrice */
    public static final int MODE_QUANTITY = 1;
    /** 按面积计价(平方): amount = lengthVal * widthVal * unitPrice */
    public static final int MODE_AREA = 2;
    /** 固定价格: amount = unitPrice (unitPrice 直接作为总价) */
    public static final int MODE_FIXED = 3;
}
