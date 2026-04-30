package com.enterprise.ad.module.customer.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 客户表 */
@Data
@TableName("crm_customer")
public class Customer {
    @TableId(type = IdType.AUTO)
    private Long id;
    @JsonProperty("name")
    private String customerName;     // 客户名称（前端使用 name 字段）
    private String contactPerson;    // 联系人
    private String phone;            // 手机号
    private String telephone;       // 座机
    private String email;           // 邮箱
    private String address;         // 地址
    private String industry;        // 行业
    // ========== 客户类型常量 ==========
    public static final int TYPE_NORMAL = 1;   // 普通客户
    public static final int TYPE_FACTORY = 2;  // 工厂客户
    public static final int TYPE_RETAIL = 3;   // 零售客户（系统级公共客户）
    private Integer customerType;
    /** 工厂类型（仅 factoryType=2 时使用）：印刷/包装/广告制作 */
    private String factoryType;
    private BigDecimal totalAmount; // 累计消费
    private Integer orderCount;     // 订单数量
    private Integer level;          // 客户等级：1普通 2VIP 3战略

    private Integer status;         // 状态：1正常 0禁用

    // ========== 会员字段（is_member=1 时启用） ==========
    private Integer isMember;       // 是否为会员：0否 1是
    private String memberLevel;     // 会员等级：normal/silver/gold/diamond
    private java.math.BigDecimal balance;         // 会员余额（预存金额）
    private java.math.BigDecimal totalRecharge;   // 累计充值
    private java.math.BigDecimal totalConsume;    // 累计消费（会员余额消费）

    private Long creatorId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic private Integer deleted;
}
