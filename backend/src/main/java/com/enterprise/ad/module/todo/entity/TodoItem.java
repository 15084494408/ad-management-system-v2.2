package com.enterprise.ad.module.todo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.FieldFill;

/** 待办事项（需求收集） */
@Data
@TableName("todo_item")
public class TodoItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String customerName;      // 客户名称
    private String contactPhone;    // 联系电话
    private String contactPerson;    // 联系人
    private String dimensions;      // 量好的尺寸
    private String requirements;     // 客户需求描述
    private Integer status;          // 状态：1新收集 2分析中 3待确认 4已转订单
    private BigDecimal quoteAmount; // AI报价金额
    private String quoteDetail;      // AI报价明细（JSON）
    private Long orderId;            // 关联订单ID
    private String orderNo;          // 关联订单编号
    private Integer priority;      // 优先级：1普通 2紧急 3加急
    private String source;           // 来源
    private String remark;           // 内部备注
    private Long creatorId;          // 创建人
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic private Integer deleted;
}
