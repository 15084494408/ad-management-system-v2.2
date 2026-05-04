package com.enterprise.ad.module.designer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.FieldFill;

/** 设计师提成配置 */
@Data
@TableName("designer_commission_config")
public class DesignerCommissionConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long designerId;         // 设计师用户ID
    private String designerName;     // 设计师姓名（冗余）
    private BigDecimal commissionRate; // 提成比例（%）
    private Integer enabled;         // 是否启用：0否 1是
    private Long updatedBy;          // 最后修改人
    private LocalDateTime updatedTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableLogic private Integer deleted;
}
