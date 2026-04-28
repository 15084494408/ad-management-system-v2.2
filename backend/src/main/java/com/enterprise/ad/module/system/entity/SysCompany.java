package com.enterprise.ad.module.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 公司信息表（支持多公司）
 */
@Data
@TableName("sys_company")
public class SysCompany {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String companyName;   // 公司名称
    private String address;       // 地址
    private String phone;         // 电话
    private String fax;           // 传真
    private String email;         // 邮箱
    private String bankName;      // 开户银行
    private String bankAccount;   // 银行账号
    private String taxNo;         // 税号
    private String logoUrl;       // Logo URL
    private Integer isDefault;    // 是否默认公司(1=是)
    private Integer status;       // 状态:0=禁用,1=启用
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
