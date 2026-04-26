package com.enterprise.ad.module.notice.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_notice")
public class Notice {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;         // 通知标题
    private String content;       // 通知内容
    private String type;         // 类型：system/system_order/finance
    private Integer level;       // 级别：1普通 2重要 3紧急
    private Long userId;         // 接收用户ID（null表示全部）
    private String userIds;      // 指定用户ID列表
    private Integer isRead;      // 是否已读：0未读 1已读
    private Long readUserId;    // 读取用户ID
    private LocalDateTime readTime; // 读取时间
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic private Integer deleted;
}
