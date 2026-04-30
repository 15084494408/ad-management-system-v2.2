package com.enterprise.ad.module.finance.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 快速记账请求 DTO
 * 支持同时提交物料明细（触发库存出库）
 */
@Data
public class CreateRecordRequest {

    @NotNull(message = "类型不能为空")
    private String type;            // income/expense

    @NotNull(message = "金额必须大于0")
    @Min(value = 1, message = "金额必须大于0")
    private BigDecimal amount;

    private String category;
    private String relatedName;
    private String paymentMethod;
    private String remark;

    /** 物料明细列表（可选，有值时触发库存出库联动） */
    private List<RecordItemRequest> items;

    @Data
    public static class RecordItemRequest {
        @NotNull(message = "物料ID不能为空")
        private Long materialId;

        private String materialName;

        /** 计价方式：0按数量 1按面积 */
        private Integer pricingType;

        /** 数量（按数量计价时使用） */
        private Integer quantity;

        /** 宽度（按面积计价时使用） */
        private BigDecimal width;

        /** 高度（按面积计价时使用） */
        private BigDecimal height;

        /** 面积（宽×高） */
        private BigDecimal area;

        /** 单价 */
        private BigDecimal unitPrice;

        /** 小计金额 */
        private BigDecimal totalPrice;
    }
}
