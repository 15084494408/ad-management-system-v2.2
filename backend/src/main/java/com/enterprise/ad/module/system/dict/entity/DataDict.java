package com.enterprise.ad.module.system.dict.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.FieldFill;

@Data
@TableName("sys_dict")
public class DataDict {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;          // 字典名称
    private String code;          // 字典编码
    private String description;   // 描述
    private Integer sortOrder;    // 排序
    private Integer status;      // 状态：1启用 0禁用
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic private Integer deleted;
}

