package com.enterprise.ad.module.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.FieldFill;

/**
 * 数据备份记录表
 */
@Data
@TableName("sys_backup")
public class SysBackup {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String fileName;       // 备份文件名
    private String filePath;       // 文件存储路径
    private Long fileSize;         // 文件大小（字节）
    private String type;           // 类型：auto/manual
    private String status;         // 状态：success/failed
    private String remark;         // 备注
    private Long operatorId;       // 操作人ID
    private String operatorName;   // 操作人名称
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
