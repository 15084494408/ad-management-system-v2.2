package com.enterprise.ad.module.square.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.FieldFill;

@Data
@TableName("sq_income")
public class SquareIncome {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long requirementId;    // 需求ID
    private String title;          // 需求标题
    private Long designerId;      // 设计师ID
    private String designerName;  // 设计师名称
    private BigDecimal quotedPrice; // 承接价格
    private BigDecimal platformFee; // 平台手续费
    private BigDecimal actualIncome; // 实际收入
    private Integer status;        // 状态：1进行中 2已结算 3已提现
    private String remark;        // 备注
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic private Integer deleted;
}
