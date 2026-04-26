package com.enterprise.ad.module.customer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 客户表 */
@Data
@TableName("crm_customer")
public class Customer {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String customerName;     // 客户名称
    private String contactPerson;    // 联系人
    private String phone;            // 手机号
    private String telephone;       // 座机
    private String email;           // 邮箱
    private String address;         // 地址
    private String industry;        // 行业
    private BigDecimal totalAmount; // 累计消费
    private Integer orderCount;     // 订单数量
    private Integer level;          // 客户等级：1普通 2VIP 3战略
    private Integer status;         // 状态：1正常 0禁用
    private Long creatorId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic private Integer deleted;
}
