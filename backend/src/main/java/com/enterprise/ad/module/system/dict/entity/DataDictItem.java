package com.enterprise.ad.module.system.dict.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.FieldFill;

@Data
@TableName("sys_dict_item")
public class DataDictItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long dictId;         // 字典ID
    private String label;        // 显示标签
    private String value;        // 值
    private Integer sortOrder;   // 排序
    private Integer status;      // 状态
    private String remark;       // 备注
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableLogic private Integer deleted;
}

