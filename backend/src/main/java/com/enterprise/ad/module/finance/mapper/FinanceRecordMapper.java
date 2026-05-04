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

@Mapper
public interface FinanceRecordMapper extends BaseMapper<FinanceRecord> {

    // ★ 修复: SQL 聚合查询，替代全量加载到 Java 内存
    @Select("SELECT COALESCE(SUM(amount), 0) FROM fin_record WHERE deleted = 0 AND type = 'income' AND create_time >= #{start} AND create_time <= #{end}")
    BigDecimal sumIncomeByRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT COALESCE(SUM(amount), 0) FROM fin_record WHERE deleted = 0 AND type = 'expense' AND create_time >= #{start} AND create_time <= #{end}")
    BigDecimal sumExpenseByRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ★ 统一流水查询（分页）— SQL 移至 FinanceRecordMapper.xml
    List<Map<String, Object>> selectAllFlow(
        @Param("startDate") String startDate,
        @Param("endDate") String endDate,
        @Param("direction") String direction,
        @Param("limit") long limit,
        @Param("offset") long offset
    );

    // ★ 统一流水总数（分页用）— SQL 移至 FinanceRecordMapper.xml
    long countAllFlow(
        @Param("startDate") String startDate,
        @Param("endDate") String endDate,
        @Param("direction") String direction
    );

    /**
     * 近30天每日收款趋势（type=income）
     * 返回: date (yyyy-MM-dd), daily_income
     */
    @Select("SELECT DATE(create_time) as date, COALESCE(SUM(amount), 0) as daily_income " +
            "FROM fin_record " +
            "WHERE deleted = 0 AND type = 'income' AND create_time >= DATE_SUB(CURDATE(), INTERVAL 30 DAY) " +
            "GROUP BY DATE(create_time) " +
            "ORDER BY date")
    List<Map<String, Object>> selectDailyIncomeLast30Days();
}
