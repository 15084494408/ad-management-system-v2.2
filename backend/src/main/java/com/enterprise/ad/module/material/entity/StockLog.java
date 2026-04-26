package com.enterprise.ad.module.material.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("mat_stock_log")
public class StockLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long materialId;       // 物料ID
    private String materialName;   // 物料名称
    private Integer changeType;    // 变动类型：1入库 2出库 3盘点
    private Integer quantity;      // 变动数量
    private Integer beforeStock;   // 变动前库存
    private Integer afterStock;    // 变动后库存
    private BigDecimal unitPrice;  // 单价
    private BigDecimal totalPrice; // 总额
    private String remark;         // 备注
    private Long operatorId;       // 操作人ID
    private String operatorName;   // 操作人
    private LocalDateTime createTime;
    @TableLogic private Integer deleted;
}
