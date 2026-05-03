package com.enterprise.ad.module.factory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.FieldFill;

/**
 * 工厂业务员表
 */
@Data
@TableName("fac_factory_salesman")
public class FactorySalesman {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;           // 业务员姓名
    private String phone;          // 联系电话
    private String email;          // 邮箱
    /** 所属工厂ID（关联 crm_customer.id where customer_type=2） */
    private Long factoryId;
    /** 工厂名称（非数据库字段，仅用于前端展示） */
    @TableField(exist = false)
    private String factoryName;
    private Integer status;        // 状态：1启用 0禁用
    private String remark;         // 备注
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic private Integer deleted;
}
