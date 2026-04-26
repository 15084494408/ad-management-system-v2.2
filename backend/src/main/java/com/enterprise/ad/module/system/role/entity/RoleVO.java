package com.enterprise.ad.module.system.role.entity;

import lombok.Data;
import java.util.List;

/**
 * 角色视图对象 - 包含关联的权限码列表
 */
@Data
public class RoleVO {
    private Long id;
    private String roleName;
    private String roleCode;
    private String description;
    private Integer sort;
    private Integer status;
    private String createTime;
    private String updateTime;
    /** 关联的权限码列表 */
    private List<String> permissions;
}
