package com.enterprise.ad.module.customer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/** 客户标签表 */
@Data
@TableName("customer_tag")
public class CustomerTag {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;        // 标签名称
    private String color;       // 标签颜色
    private String icon;        // 标签图标
    private String description; // 标签说明
    private Integer sort;       // 排序
    private Integer status;     // 状态 1正常 0禁用
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
