package com.enterprise.ad.module.customer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 客户等级表 */
@Data
@TableName("customer_level")
public class CustomerLevel {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;           // 等级名称
    private Integer level;         // 等级权重（数值越大等级越高）
    private BigDecimal minAmount;  // 消费门槛
    private Integer discount;      // 折扣比例（如90=9折，100=原价）
    private String description;    // 等级说明
    private Integer sort;          // 排序
    private Integer status;        // 状态 1正常 0禁用
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
