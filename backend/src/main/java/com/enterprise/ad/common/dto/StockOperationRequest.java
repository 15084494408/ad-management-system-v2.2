package com.enterprise.ad.common.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 库存操作请求 DTO
 * ★ 替代 MaterialController.stockIn/stockOut 的 Map<String, Object> params
 */
@Data
public class StockOperationRequest {

    @NotNull(message = "物料ID不能为空")
    private Long materialId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "操作数量必须大于0")
    private Integer quantity;

    /** 备注 */
    private String remark;
}
