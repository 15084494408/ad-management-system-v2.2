package com.enterprise.ad.module.system.role.entity;

import lombok.Data;
import java.util.List;

/**
 * 角色创建/更新请求 - 包含基本信息和权限码列表
 */
@Data
public class RoleRequest {
    private String roleName;
    private String roleCode;
    private String description;
    private Integer sort;
    /** 要分配的权限码列表，如 ["order:view", "order:create"] */
    private List<String> permissions;
}
