package com.enterprise.ad.module.system.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import com.baomidou.mybatisplus.annotation.FieldFill;

/**
 * 系统权限实体（菜单 + 按钮/接口权限）
 */
@Data
@TableName("sys_permission")
public class SysPermission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;            // 父ID（0为根）
    private String name;              // 权限名称
    private String type;              // 类型：menu/button/interface
    private String path;              // 路由路径
    private String component;         // 前端组件路径
    private String icon;              // 图标
    private String permissionCode;    // 权限码（如 order:create）
    private Integer sort;             // 排序
    private Integer visible;           // 是否显示：0隐藏 1显示
    private Integer status;           // 状态：0禁用 1正常
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

    /** 菜单树子节点（非数据库字段） */
    @TableField(exist = false)
    private List<SysPermission> children;
}
