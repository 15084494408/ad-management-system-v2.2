package com.enterprise.ad.module.factory.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.math.BigDecimal;

/**
 * 创建工厂/客户账单请求 DTO
 * ★ 修复 P0-1: 替代直接使用 FactoryBill Entity 接收请求
 */
@Data
public class CreateFactoryBillDTO {

    /** 客户ID（关联crm_customer，customer_type=2为工厂客户） */
    private Long customerId;

    /** 原工厂ID（保留兼容） */
    private Long factoryId;

    /** 业务员ID */
    private Long salesmanId;

    /** 账单类型：1=工厂账单 2=客户账单，默认1 */
    private Integer billType = 1;

    @NotBlank(message = "账单月份不能为空")
    private String month;

    /** 账单总额 */
    private BigDecimal totalAmount;

    /** 对账文件URL */
    private String reconcileFile;

    /** 备注 */
    private String remark;
}
