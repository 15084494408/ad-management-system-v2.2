package com.enterprise.ad.module.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.ad.module.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 查询指定日期前缀的最大订单编号（如 DD20260427），用于生成不重复的订单号
     */
    @Select("SELECT MAX(order_no) FROM ord_order WHERE order_no LIKE CONCAT(#{prefix}, '%') AND deleted = 0")
    String selectMaxOrderNo(@Param("prefix") String prefix);

    // ★ 修复 P2-14: SQL 聚合查询，替代全量加载到 Java 中计算

    @Select("SELECT COUNT(*) FROM ord_order WHERE deleted = 0 AND create_time >= #{start} AND create_time <= #{end}")
    long countByTimeRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT COUNT(*) FROM ord_order WHERE deleted = 0 AND status = #{status} AND create_time >= #{start} AND create_time <= #{end}")
    long countByStatusAndTimeRange(@Param("status") int status, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT COALESCE(SUM(total_amount), 0) FROM ord_order WHERE deleted = 0 AND create_time >= #{start} AND create_time <= #{end}")
    BigDecimal sumTotalAmount(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT COALESCE(SUM(paid_amount), 0) FROM ord_order WHERE deleted = 0 AND create_time >= #{start} AND create_time <= #{end}")
    BigDecimal sumPaidAmount(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 按日统计订单数（近 N 天趋势）
     */
    @Select("SELECT DATE(create_time) as date, COUNT(*) as count " +
            "FROM ord_order WHERE deleted = 0 AND create_time >= #{start} AND create_time <= #{end} " +
            "GROUP BY DATE(create_time) ORDER BY date")
    List<Map<String, Object>> countByDay(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ★ 修复 P1-11: 应收应付 SQL 聚合

    @Select("SELECT COALESCE(SUM(total_amount), 0) FROM ord_order WHERE deleted = 0")
    BigDecimal sumAllTotalAmount();

    @Select("SELECT COALESCE(SUM(paid_amount), 0) FROM ord_order WHERE deleted = 0")
    BigDecimal sumAllPaidAmount();
}
