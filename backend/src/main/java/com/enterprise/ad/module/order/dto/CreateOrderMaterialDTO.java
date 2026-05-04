package com.enterprise.ad.module.order.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 创建/更新订单物料明细 DTO
 * 修复 P0-1: 替代直接使用 OrderMaterial Entity 接收请求
 */
@Data
public class CreateOrderMaterialDTO {

    /** 物料名称 */
    private String materialName;

    /** 规格 */
    private String spec;

    /** 单位 */
    private String unit;

    /** 数量 */
    private BigDecimal quantity;

    /** 单价 */
    private BigDecimal unitPrice;

    /** 单位成本（管理员填写，财务/超管可见）新增字段 */
    private BigDecimal unitCost;

    /** 金额（数量 × 单价，可选） */
    private BigDecimal amount;

    /** 备注 */
    private String remark;
}