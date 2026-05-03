package com.enterprise.ad.module.design.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.FieldFill;

@Data
@TableName("des_file_version")
public class FileVersion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long fileId;          // 文件ID
    private Integer version;       // 版本号
    private String path;           // 版本文件路径
    private String url;            // 版本文件URL
    private Long size;            // 版本文件大小
    private String changeDesc;    // 变更说明
    private Long operatorId;      // 操作人ID
    private String operatorName;  // 操作人
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableLogic private Integer deleted;
}
