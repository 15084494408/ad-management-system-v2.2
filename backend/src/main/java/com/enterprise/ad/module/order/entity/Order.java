package com.enterprise.ad.module.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 订单主表 */
@Data
@TableName("ord_order")
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;          // 订单编号（系统生成）
    private Long customerId;          // 客户ID
    private String customerName;      // 客户名称（冗余）
    private Long memberId;            // 会员ID（可选）
    private String title;             // 订单标题
    private String description;       // 订单描述
    private Integer orderType;        // 订单类型：1印刷 2广告 3设计
    private BigDecimal totalAmount;  // 订单总额
    private BigDecimal totalCost;    // 总成本（管理员填写）
    private BigDecimal designerCommission; // 设计师提成（暂不启用，默认0）
    private BigDecimal paidAmount;   // 已付金额
    private BigDecimal discountAmount; // 优惠金额
    private BigDecimal roundingAmount; // 抹零金额（负数表示减免）
    private BigDecimal memberDeductAmount; // 会员余额抵扣金额
    private Integer paymentStatus;   // 支付状态：1未付 2部分付 3已付清 4已抹零结清
    private String contactPerson;     // 联系人
    private String contactPhone;     // 联系电话
    private String deliveryAddress;  // 交付地址
    private LocalDate deliveryDate;   // 交付日期
    private String designerName;     // 设计师
    private Long designerId;         // 设计师用户ID
    private Integer priority;        // 优先级：1普通 2紧急 3加急
    private Integer status;           // 订单状态：1待处理 2进行中 3已完成 4已取消
    private Integer source;          // 来源：1门店创建 2设计广场
    private BigDecimal quoteAmount;  // 报价金额
    private BigDecimal depositAmount;// 定金金额
    private String payMethod;        // 付款方式
    private String invoiceType;      // 开票类型
    private String remark;            // 备注
    private Long creatorId;          // 创建人ID
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic private Integer deleted;

    /** 不映射数据库，仅用于创建订单时传入物料明细列表 */
    @TableField(exist = false)
    private List<OrderMaterial> materials;
}
