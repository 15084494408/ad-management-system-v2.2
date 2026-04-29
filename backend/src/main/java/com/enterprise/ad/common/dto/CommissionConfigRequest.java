package com.enterprise.ad.common.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 设计师提成配置请求 DTO
 * ★ 替代 DesignerCommissionController.config() 的 Map<String, Object> body
 */
@Data
public class CommissionConfigRequest {

    @NotNull(message = "设计师ID不能为空")
    private Long designerId;

    @NotNull(message = "提成比例不能为空")
    @DecimalMin(value = "0", message = "提成比例不能为负数")
    @DecimalMax(value = "100", message = "提成比例不能超过100%")
    private BigDecimal commissionRate;

    /** 是否启用（默认true） */
    private Boolean enabled = true;
}
