package com.enterprise.ad.module.notice.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.FieldFill;

@Data
@TableName("sys_notice_setting")
public class NoticeSetting {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;         // 用户ID
    private Integer orderNotify; // 订单通知：1开启 0关闭
    private Integer financeNotify; // 财务通知：1开启 0关闭
    private Integer systemNotify;  // 系统通知：1开启 0关闭
    private Integer warningNotify; // 预警通知：1开启 0关闭
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic private Integer deleted;
}
