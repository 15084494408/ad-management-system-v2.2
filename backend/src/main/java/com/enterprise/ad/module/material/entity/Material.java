package com.enterprise.ad.module.material.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("mat_material")
public class Material {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;              // 物料名称
    private String code;              // 物料编码
    private Long categoryId;          // 分类ID
    private String categoryName;      // 分类名称（冗余）
    private String spec;              // 规格
    private String unit;              // 单位
    private BigDecimal price;        // 零售价
    private BigDecimal costPrice;    // 成本价
    private BigDecimal factoryPrice;  // 工厂价
    private Integer stockQuantity;    // 库存数量
    private Integer warningQuantity;  // 预警数量
    private Integer minQuantity;     // 最小库存
    private Integer status;           // 状态：1正常 0禁用
    private String remark;            // 备注
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic private Integer deleted;
}
