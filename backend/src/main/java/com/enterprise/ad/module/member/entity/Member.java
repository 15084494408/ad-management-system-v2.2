package com.enterprise.ad.module.member.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.FieldFill;

/** 会员表 */
@Data
@TableName("mem_member")
public class Member {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String memberName;      // 会员名称（公司/个人名）
    private String contactPerson;   // 联系人
    private String phone;           // 手机号
    private String level;           // 等级：normal/silver/gold/diamond
    private BigDecimal balance;     // 账户余额（预存金额）
    private BigDecimal totalRecharge; // 累计充值
    private BigDecimal totalConsume; // 累计消费
    private Integer status;         // 状态：1正常 0禁用
    private Long creatorId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic private Integer deleted;

    /** 以下字段不映射数据库，由接口动态填充 */
    @TableField(exist = false)
    private String contact;          // 联系人（前端字段，映射到 contactPerson）
    @TableField(exist = false)
    private String paymentMethod;    // 默认充值方式
    @TableField(exist = false)
    private String remark;           // 备注
    @TableField(exist = false)
    private Long orderCount;         // 关联订单数
}
