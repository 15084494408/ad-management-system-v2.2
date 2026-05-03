package com.enterprise.ad.module.system.log.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.FieldFill;

@Data
@TableName("sys_operation_log")
public class OperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String module;         // 操作模块
    private String action;        // 操作类型
    private String description;    // 操作描述
    private String method;        // 请求方法
    private String url;           // 请求URL
    private String param;         // 请求参数
    private String result;        // 返回结果
    private Long userId;          // 操作人ID
    private String username;      // 操作人用户名
    private String ip;            // IP地址
    private String userAgent;     // UserAgent
    private Integer status;       // 状态：1成功 0失败
    private Long duration;        // 执行时长(ms)
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableLogic private Integer deleted;
}
