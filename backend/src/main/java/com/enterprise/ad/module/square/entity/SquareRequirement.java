package com.enterprise.ad.module.square.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("sq_requirement")
public class SquareRequirement {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;           // 需求标题
    private String description;      // 需求描述
    private String category;        // 需求类型：poster/logo/brochure/package/other
    private BigDecimal budget;      // 预算
    private String budgetDesc;      // 预算说明
    private LocalDateTime deadline; // 截止日期
    private String attachment;     // 附件（JSON数组）
    private Long publisherId;       // 发布人ID
    private String publisherName;   // 发布人名称
    private Long customerId;       // 关联客户ID
    private String customerName;   // 关联客户名称
    private Integer status;        // 状态：1招募中 2设计中 3已完成 4已取消
    private Integer viewCount;     // 浏览次数
    private Integer applyCount;     // 申请次数
    private Long designerId;       // 承接设计师ID
    private String designerName;   // 承接设计师名称
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic private Integer deleted;
}
