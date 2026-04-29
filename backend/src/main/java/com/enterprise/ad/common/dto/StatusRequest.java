package com.enterprise.ad.common.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 通用状态变更请求 DTO
 * ★ 替代各 Controller 中的 Map<String, Integer> body
 */
@Data
public class StatusRequest {

    @NotNull(message = "状态值不能为空")
    private Integer status;
}
