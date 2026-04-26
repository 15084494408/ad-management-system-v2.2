package com.enterprise.ad.module.design.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("des_file")
public class DesignFile {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;           // 文件名
    private String originalName;   // 原始文件名
    private String path;           // 存储路径
    private String url;            // 访问URL
    private Long size;            // 文件大小(字节)
    private String extension;      // 扩展名
    private String mimeType;       // MIME类型
    private Long orderId;         // 关联订单ID
    private Long uploaderId;      // 上传人ID
    private String uploaderName;  // 上传人
    private Integer version;       // 版本号
    private String description;    // 文件描述
    private Integer status;        // 状态：1待审核 2通过 3驳回
    private String remark;        // 审核备注
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic private Integer deleted;
}
