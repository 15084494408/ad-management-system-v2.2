package com.enterprise.ad.module.system.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 系统用户实体
 */
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;          // 用户名（唯一）
    private String password;          // 密码（加密存储）
    private String realName;          // 真实姓名
    private String phone;             // 手机号
    private String email;             // 邮箱
    private String avatar;            // 头像URL
    private Integer status;           // 状态：0禁用 1正常
    private Long companyId;           // 所属公司ID（支持多公司）
    private String department;        // 部门
    private Long createBy;            // 创建人ID
    private LocalDateTime createTime; // 创建时间
    private Long updateBy;            // 更新人ID
    private LocalDateTime updateTime;  // 更新时间

    @TableLogic
    private Integer deleted;          // 逻辑删除：0未删 1已删
}
