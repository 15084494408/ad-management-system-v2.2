package com.enterprise.ad.module.system.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

/**
 * 创建系统用户请求 DTO
 * ★ 修复 P0-1: 替代直接使用 SysUser Entity 接收请求
 */
@Data
public class CreateUserDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    private String realName;

    private String phone;

    private String email;

    private String avatar;

    /** 状态：0禁用 1正常，默认1 */
    private Integer status = 1;

    /** 角色ID列表 */
    private List<Long> roleIds;
}
