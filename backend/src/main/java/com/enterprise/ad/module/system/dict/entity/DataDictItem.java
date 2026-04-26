package com.enterprise.ad.module.system.dict.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

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
    private LocalDateTime createTime;
    @TableLogic private Integer deleted;
}

