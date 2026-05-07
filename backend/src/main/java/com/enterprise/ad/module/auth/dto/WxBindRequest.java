package com.enterprise.ad.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 微信绑定请求DTO（已登录用户绑定微信）
 */
@Data
public class WxBindRequest {
    @NotBlank(message = "微信code不能为空")
    private String code;
}
