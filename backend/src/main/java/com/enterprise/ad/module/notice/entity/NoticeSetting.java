package com.enterprise.ad.module.notice.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

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
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic private Integer deleted;
}
