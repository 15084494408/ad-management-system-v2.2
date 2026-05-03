package com.enterprise.ad.module.system.role.entity;

import lombok.Data;
import java.util.List;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
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
    @TableField(fill = FieldFill.INSERT)
    private String createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateTime;
    /** 关联的权限码列表 */
    private List<String> permissions;
}
