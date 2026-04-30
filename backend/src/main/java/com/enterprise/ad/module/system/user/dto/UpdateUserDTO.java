package com.enterprise.ad.module.system.user.dto;

import lombok.Data;
import java.util.List;

/**
 * 更新系统用户请求 DTO
 * ★ 修复 P0-1: 替代直接使用 SysUser Entity 接收请求
 */
@Data
public class UpdateUserDTO {

    private String realName;

    private String phone;

    private String email;

    private String avatar;

    /** 状态：0禁用 1正常 */
    private Integer status;

    /** 角色ID列表（更新时覆盖原有角色） */
    private List<Long> roleIds;
}
