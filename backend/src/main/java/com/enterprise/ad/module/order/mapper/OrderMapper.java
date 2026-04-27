package com.enterprise.ad.module.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.ad.module.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 查询指定日期前缀的最大订单编号（如 DD20260427），用于生成不重复的订单号
     */
    @Select("SELECT MAX(order_no) FROM ord_order WHERE order_no LIKE CONCAT(#{prefix}, '%') AND deleted = 0")
    String selectMaxOrderNo(@Param("prefix") String prefix);
}
