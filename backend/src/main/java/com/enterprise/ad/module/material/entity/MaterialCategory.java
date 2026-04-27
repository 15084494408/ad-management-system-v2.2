package com.enterprise.ad.module.material.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("mat_category")
public class MaterialCategory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;          // 分类名称
    private String code;          // 分类编码
    private String icon;          // 分类图标
    private Long parentId;        // 父级ID
    private Integer sortOrder;    // 排序
    private String description;   // 分类说明
    private Integer status;       // 状态：1正常 0禁用
    private LocalDateTime createTime;
    @TableLogic private Integer deleted;
}
