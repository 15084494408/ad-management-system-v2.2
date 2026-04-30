package com.enterprise.ad.module.factory.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 更新工厂/客户账单请求 DTO
 * ★ 修复 P0-1: 替代直接使用 FactoryBill Entity 接收请求
 */
@Data
public class UpdateFactoryBillDTO {

    /** 客户ID */
    private Long customerId;

    /** 原工厂ID */
    private Long factoryId;

    /** 业务员ID */
    private Long salesmanId;

    /** 账单类型：1=工厂账单 2=客户账单 */
    private Integer billType;

    /** 账单月份 */
    private String month;

    /** 账单总额 */
    private BigDecimal totalAmount;

    /** 状态：1未对账 2已对账 3部分付款 4已结清 */
    private Integer status;

    /** 对账文件URL */
    private String reconcileFile;

    /** 备注 */
    private String remark;
}
