package com.enterprise.ad.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 微信登录请求DTO
 */
@Data
public class WxLoginRequest {
    @NotBlank(message = "微信code不能为空")
    private String code;
}
