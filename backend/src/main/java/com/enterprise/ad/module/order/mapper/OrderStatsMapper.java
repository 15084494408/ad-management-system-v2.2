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

/**
 * ★ 新增 SQL 聚合方法，消除 StatisticsController 全量加载到内存的问题
 */
@Mapper
public interface OrderStatsMapper {

    @Select("SELECT COUNT(*) FROM ord_order WHERE deleted = 0 AND create_time >= #{start} AND create_time <= #{end}")
    long countByRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT COALESCE(SUM(total_amount), 0) FROM ord_order WHERE deleted = 0 AND create_time >= #{start} AND create_time <= #{end}")
    BigDecimal sumTotalAmountByRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT COALESCE(SUM(paid_amount), 0) FROM ord_order WHERE deleted = 0 AND create_time >= #{start} AND create_time <= #{end}")
    BigDecimal sumPaidAmountByRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT COALESCE(SUM(total_amount), 0) FROM ord_order WHERE deleted = 0 AND status = #{status} AND create_time >= #{start} AND create_time <= #{end}")
    BigDecimal sumAmountByStatusAndRange(@Param("status") int status, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT COUNT(*) FROM ord_order WHERE deleted = 0 AND status = #{status} AND create_time >= #{start} AND create_time <= #{end}")
    long countByStatusAndRange(@Param("status") int status, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 按日/周/月分组统计订单数和金额
     */
    @Select("<script>" +
            "SELECT " +
            "  CASE " +
            "    WHEN #{period} = 'daily' THEN DATE_FORMAT(create_time, '%Y-%m-%d') " +
            "    WHEN #{period} = 'weekly' THEN DATE_FORMAT(DATE_SUB(create_time, INTERVAL WEEKDAY(create_time) DAY), '%Y-%m-%d') " +
            "    ELSE DATE_FORMAT(create_time, '%Y-%m-01') " +
            "  END AS date_key, " +
            "  COUNT(*) AS count, " +
            "  COALESCE(SUM(total_amount), 0) AS amount " +
            "FROM ord_order WHERE deleted = 0 AND create_time >= #{start} AND create_time &lt;= #{end} " +
            "GROUP BY date_key ORDER BY date_key" +
            "</script>")
    List<Map<String, Object>> groupByPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("period") String period);
}
