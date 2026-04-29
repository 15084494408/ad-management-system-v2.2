package com.enterprise.ad.module.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.ad.module.finance.entity.FinanceRecord;
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
public interface FinanceRecordStatsMapper extends BaseMapper<FinanceRecord> {

    @Select("SELECT COUNT(*) FROM fin_record WHERE deleted = 0 AND create_time >= #{start} AND create_time <= #{end}")
    long countByRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT COUNT(*) FROM fin_record WHERE deleted = 0 AND type = #{type} AND create_time >= #{start} AND create_time <= #{end}")
    long countByTypeAndRange(@Param("type") String type, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("<script>" +
            "SELECT COALESCE(SUM(amount), 0) FROM fin_record " +
            "WHERE deleted = 0 AND type = #{type} AND create_time >= #{start} AND create_time &lt;= #{end} " +
            "<if test='paymentMethod != null'>AND payment_method = #{paymentMethod}</if>" +
            "</script>")
    BigDecimal sumByTypeRange(@Param("type") String type, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end,
                              @Param("paymentMethod") String paymentMethod);

    /**
     * 按日/周/月分组统计收支
     */
    @Select("<script>" +
            "SELECT " +
            "  CASE " +
            "    WHEN #{period} = 'daily' THEN DATE_FORMAT(create_time, '%Y-%m-%d') " +
            "    WHEN #{period} = 'weekly' THEN DATE_FORMAT(DATE_SUB(create_time, INTERVAL WEEKDAY(create_time) DAY), '%Y-%m-%d') " +
            "    ELSE DATE_FORMAT(create_time, '%Y-%m-01') " +
            "  END AS date_key, " +
            "  SUM(CASE WHEN type = 'income' THEN amount ELSE 0 END) AS income, " +
            "  SUM(CASE WHEN type = 'expense' THEN amount ELSE 0 END) AS expense, " +
            "  COUNT(*) AS count " +
            "FROM fin_record WHERE deleted = 0 AND create_time >= #{start} AND create_time &lt;= #{end} " +
            "GROUP BY date_key ORDER BY date_key" +
            "</script>")
    List<Map<String, Object>> groupByPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("period") String period);

    /**
     * 按类型分组统计
     */
    @Select("SELECT IFNULL(category, '其他') AS category, " +
            "COALESCE(SUM(amount), 0) AS amount, COUNT(*) AS count " +
            "FROM fin_record WHERE deleted = 0 AND type = #{type} " +
            "AND create_time >= #{start} AND create_time <= #{end} " +
            "GROUP BY category ORDER BY amount DESC")
    List<Map<String, Object>> groupByCategory(@Param("type") String type, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 按支付方式分组统计（收入渠道构成）
     */
    @Select("SELECT IFNULL(payment_method, '其他') AS channel, " +
            "COALESCE(SUM(amount), 0) AS amount " +
            "FROM fin_record WHERE deleted = 0 AND type = 'income' " +
            "AND create_time >= #{start} AND create_time <= #{end} " +
            "GROUP BY payment_method ORDER BY amount DESC")
    List<Map<String, Object>> groupByPaymentChannel(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
