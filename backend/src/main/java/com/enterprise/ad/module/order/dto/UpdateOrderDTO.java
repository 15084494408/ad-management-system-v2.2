package com.enterprise.ad.module.order.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 更新订单请求 DTO
 * ★ 修复 P0-1: 替代直接使用 Order Entity 接收请求，防止前端传入非法字段
 */
@Data
public class UpdateOrderDTO {

    private Long customerId;

    private String customerName;

    private Long memberId;

    private String title;

    private String description;

    /** 订单类型：1印刷 2广告 3设计 */
    private Integer orderType;

    /** 订单总额 */
    private BigDecimal totalAmount;

    /** 优惠金额 */
    private BigDecimal discountAmount;

    /** 抹零金额 */
    private BigDecimal roundingAmount;

    /** 联系人 */
    private String contactPerson;

    /** 联系电话 */
    private String contactPhone;

    /** 交付地址 */
    private String deliveryAddress;

    /** 交付日期 */
    private LocalDate deliveryDate;

    /** 设计师用户ID */
    private Long designerId;

    /** 优先级：1普通 2紧急 3加急 */
    private Integer priority;

    /** 订单状态：1待处理 2进行中 3已完成 4已取消 */
    private Integer status;

    /** 报价金额 */
    private BigDecimal quoteAmount;

    /** 定金金额 */
    private BigDecimal depositAmount;

    /** 付款方式 */
    private String payMethod;

    /** 开票类型 */
    private String invoiceType;

    /** 备注 */
    private String remark;

    /** 物料明细列表（更新时传入将覆盖原有物料） */
    private List<CreateOrderMaterialDTO> materials;
}
