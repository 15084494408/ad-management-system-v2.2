package com.enterprise.ad.module.square.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.FieldFill;

@Data
@TableName("sq_application")
public class SquareApplication {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long requirementId;    // 需求ID
    private Long designerId;      // 设计师ID
    private String designerName;  // 设计师名称
    private String proposal;      // 申请说明/提案
    private BigDecimal quotedPrice; // 报价
    private Integer status;       // 状态：1待审核 2已接受 3已拒绝
    private String rejectReason; // 拒绝原因
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic private Integer deleted;
}
