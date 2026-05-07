package com.enterprise.ad.module.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 工资管理表 */
@Data
@TableName("fin_salary")
public class Salary {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long employeeId;           // 员工用户ID
    private String employeeName;       // 员工姓名
    private String month;              // 工资月份 (YYYY-MM)
    private BigDecimal baseSalary;     // 基本工资
    private BigDecimal commissionAmount; // 提成金额（自动汇总）
    private BigDecimal bonus;          // 奖金
    private BigDecimal deduction;      // 扣款
    private BigDecimal totalAmount;    // 实发合计
    private Integer status;            // 状态：1待发放 2已发放
    private String remark;             // 备注
    private Long creatorId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
