package com.enterprise.ad.module.factory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 工厂表 */
@Data
@TableName("fac_factory")
public class Factory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String factoryName;     // 工厂名称
    private String contactPerson;   // 联系人
    private String phone;           // 电话
    private String address;         // 地址
    private String type;            // 类型：印刷/包装/广告制作
    private Integer status;         // 状态：1正常 0暂停
    private Long creatorId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic private Integer deleted;
}
