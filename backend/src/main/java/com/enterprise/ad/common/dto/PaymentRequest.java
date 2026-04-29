package com.enterprise.ad.common.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 收款请求 DTO
 * ★ 替代 OrderController.addPayment() 的 Map<String, Object> body
 */
@Data
public class PaymentRequest {

    @NotNull(message = "收款金额不能为空")
    @DecimalMin(value = "0.01", message = "收款金额必须大于0")
    private BigDecimal amount;

    /** 是否抹零结清 */
    private Boolean writeOff = false;

    /** 支付方式 */
    private String method;

    /** 付款人 */
    private String payer;

    /** 备注 */
    private String remark;
}
