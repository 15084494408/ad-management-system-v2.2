package com.enterprise.ad.module.system.role.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/** 系统角色表 */
@Data
@TableName("sys_role")
public class Role {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String roleName;      // 角色名称
    private String roleCode;      // 角色编码
    private String description;   // 描述
    private Integer sort;         // 排序
    private Integer status;       // 状态：0禁用 1正常
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
