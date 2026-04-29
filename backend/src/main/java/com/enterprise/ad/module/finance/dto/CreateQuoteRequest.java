package com.enterprise.ad.module.finance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * ★ 修复 P1-10: 创建报价请求 DTO
 * 替代原来的 Map<String, Object>，提供类型安全和参数校验
 */
@Data
public class CreateQuoteRequest {

    @NotBlank(message = "客户名称不能为空")
    private String customerName;

    private Long customerId;

    private String projectName;

    private Long companyId;

    /** 折扣率（默认100） */
    private BigDecimal discount = BigDecimal.valueOf(100);

    /** 税率 */
    private BigDecimal taxRate;

    /** 报价有效期 */
    private String validUntil;

    /** 报价日期 */
    private String quoteDate;

    private String remark;

    /** 合计金额 */
    private BigDecimal totalAmount;

    /** 最终金额（含税/折扣后） */
    private BigDecimal finalAmount;

    /** 税额 */
    private BigDecimal taxAmount;

    /** 报价明细 */
    @NotEmpty(message = "报价明细不能为空")
    @Valid
    private List<QuoteDetailRequest> details;

    @Data
    public static class QuoteDetailRequest {
        @NotBlank(message = "物料名称不能为空")
        private String materialName;

        private String spec;
        private String unit;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal amount;
        private String remark;
        /** 是否自定义物料 */
        private Integer isCustom = 0;
    }
}
