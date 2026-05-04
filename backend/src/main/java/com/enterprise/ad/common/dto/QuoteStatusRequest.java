package com.enterprise.ad.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

/**
 * 报价状态变更请求 DTO
 * ★ 替代 FinanceController.updateQuoteStatus() 的 Map<String, String> body
 */
@Data
public class QuoteStatusRequest {

    @NotBlank(message = "状态值不能为空")
    @Pattern(regexp = "pending|accepted|rejected|expired", message = "无效的状态值，允许: pending/accepted/rejected/expired")
    private String status;
}
