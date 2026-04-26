package com.enterprise.ad.module.system.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 按钮/权限资源表
 */
@Data
@TableName("sys_button")
public class SysButton {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;           // 按钮名称
    private String permission;     // 权限标识，如 system:user:add
    private String type;           // 类型：button/menu/api
    private Long parentId;         // 上级菜单ID
    private Integer sort;          // 排序
    private Integer status;        // 状态：1启用 0停用
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
