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

    /**
     * 统一流水查询：聚合 fin_record(快速记账+订单收款) + mem_member_transaction(会员充值/消费)
     *               + fac_factory_bill(工厂付款) + sq_income(设计广场收入)
     * 返回统一结构: source, recordNo, direction(income/expense), amount, category,
     *               relatedName, paymentMethod, remark, createTime
     */
    @Select("<script>" +
            "SELECT * FROM (" +
            "  -- 1. 快速记账 fin_record" +
            "  SELECT " +
            "    'quick_record' AS source, " +
            "    record_no AS record_no, " +
            "    type AS direction, " +
            "    amount, " +
            "    IFNULL(category, '其他') AS category, " +
            "    IFNULL(related_name, '') AS related_name, " +
            "    IFNULL(payment_method, '') AS payment_method, " +
            "    IFNULL(remark, '') AS remark, " +
            "    create_time" +
            "  FROM fin_record WHERE deleted = 0 " +
            "  <if test='startDate != null'>" +
            "    AND create_time &gt;= #{startDate}" +
            "  </if>" +
            "  <if test='endDate != null'>" +
            "    AND create_time &lt;= #{endDate}" +
            "  </if>" +
            "  <if test='direction != null'>" +
            "    AND type = #{direction}" +
            "  </if>" +

            "  UNION ALL " +

            "  -- 2. 会员充值 mem_member_transaction" +
            "  SELECT " +
            "    'member_recharge' AS source, " +
            "    CONCAT('MR', id) AS record_no, " +
            "    'income' AS direction, " +
            "    amount, " +
            "    '会员充值' AS category, " +
            "    IFNULL((SELECT name FROM mem_member WHERE id = mem_member_transaction.member_id), '未知会员') AS related_name, " +
            "    '' AS payment_method, " +
            "    IFNULL(remark, '') AS remark, " +
            "    create_time" +
            "  FROM mem_member_transaction WHERE deleted = 0 AND type = 'recharge' " +
            "  <if test='startDate != null'>" +
            "    AND create_time &gt;= #{startDate}" +
            "  </if>" +
            "  <if test='endDate != null'>" +
            "    AND create_time &lt;= #{endDate}" +
            "  </if>" +
            "  <if test='direction != null and direction == \"income\"'>" +
            "    AND 1=1 " +
            "  </if>" +

            "  UNION ALL " +

            "  -- 3. 会员消费 mem_member_transaction" +
            "  SELECT " +
            "    'member_consume' AS source, " +
            "    CONCAT('MC', id) AS record_no, " +
            "    'expense' AS direction, " +
            "    amount, " +
            "    '会员消费' AS category, " +
            "    IFNULL((SELECT name FROM mem_member WHERE id = mem_member_transaction.member_id), '未知会员') AS related_name, " +
            "    '' AS payment_method, " +
            "    IFNULL(remark, '') AS remark, " +
            "    create_time" +
            "  FROM mem_member_transaction WHERE deleted = 0 AND type = 'consume' " +
            "  <if test='startDate != null'>" +
            "    AND create_time &gt;= #{startDate}" +
            "  </if>" +
            "  <if test='endDate != null'>" +
            "    AND create_time &lt;= #{endDate}" +
            "  </if>" +
            "  <if test='direction != null and direction == \"expense\"'>" +
            "    AND 1=1 " +
            "  </if>" +

            "  UNION ALL " +

            "  -- 4. 工厂付款 fac_factory_bill（仅展示已结清/部分付款的账单）" +
            "  SELECT " +
            "    'factory_bill' AS source, " +
            "    bill_no AS record_no, " +
            "    'expense' AS direction, " +
            "    IFNULL(paid_amount, 0) AS amount, " +
            "    '工厂账单' AS category, " +
            "    IFNULL(factory_name, '') AS related_name, " +
            "    '' AS payment_method, " +
            "    CONCAT('月份: ', month) AS remark, " +
            "    create_time" +
            "  FROM fac_factory_bill WHERE deleted = 0 AND IFNULL(paid_amount, 0) > 0 " +
            "  <if test='startDate != null'>" +
            "    AND create_time &gt;= #{startDate}" +
            "  </if>" +
            "  <if test='endDate != null'>" +
            "    AND create_time &lt;= #{endDate}" +
            "  </if>" +
            "  <if test='direction != null and direction == \"expense\"'>" +
            "    AND 1=1 " +
            "  </if>" +

            "  UNION ALL " +

            "  -- 5. 设计广场收入 sq_income（已结算）" +
            "  SELECT " +
            "    'square_income' AS source, " +
            "    CONCAT('SQ', id) AS record_no, " +
            "    'income' AS direction, " +
            "    IFNULL(quoted_price, 0) - IFNULL(actual_income, 0) AS amount, " +
            "    '设计广场收入' AS category, " +
            "    IFNULL(designer_name, '') AS related_name, " +
            "    '' AS payment_method, " +
            "    CONCAT('需求: ', IFNULL(title, '')) AS remark, " +
            "    create_time" +
            "  FROM sq_income WHERE deleted = 0 AND status = 2 " +
            "    AND IFNULL(quoted_price, 0) - IFNULL(actual_income, 0) > 0 " +
            "  <if test='startDate != null'>" +
            "    AND create_time &gt;= #{startDate}" +
            "  </if>" +
            "  <if test='endDate != null'>" +
            "    AND create_time &lt;= #{endDate}" +
            "  </if>" +
            "  <if test='direction != null and direction == \"income\"'>" +
            "    AND 1=1 " +
            "  </if>" +

            ") all_flow ORDER BY create_time DESC" +
            " LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    List<Map<String, Object>> selectAllFlow(
        @Param("startDate") String startDate,
        @Param("endDate") String endDate,
        @Param("direction") String direction,
        @Param("limit") long limit,
        @Param("offset") long offset
    );

    /**
     * 统一流水总数（用于分页）
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM (" +
            "  SELECT id FROM fin_record WHERE deleted = 0 " +
            "  <if test='startDate != null'>AND create_time &gt;= #{startDate}</if>" +
            "  <if test='endDate != null'>AND create_time &lt;= #{endDate}</if>" +
            "  <if test='direction != null'>AND type = #{direction}</if>" +
            "  UNION ALL " +
            "  SELECT id FROM mem_member_transaction WHERE deleted = 0 AND type = 'recharge' " +
            "  <if test='startDate != null'>AND create_time &gt;= #{startDate}</if>" +
            "  <if test='endDate != null'>AND create_time &lt;= #{endDate}</if>" +
            "  <if test='direction != null and direction == \"income\"'>AND 1=1</if>" +
            "  UNION ALL " +
            "  SELECT id FROM mem_member_transaction WHERE deleted = 0 AND type = 'consume' " +
            "  <if test='startDate != null'>AND create_time &gt;= #{startDate}</if>" +
            "  <if test='endDate != null'>AND create_time &lt;= #{endDate}</if>" +
            "  <if test='direction != null and direction == \"expense\"'>AND 1=1</if>" +
            "  UNION ALL " +
            "  SELECT id FROM fac_factory_bill WHERE deleted = 0 AND IFNULL(paid_amount, 0) > 0 " +
            "  <if test='startDate != null'>AND create_time &gt;= #{startDate}</if>" +
            "  <if test='endDate != null'>AND create_time &lt;= #{endDate}</if>" +
            "  <if test='direction != null and direction == \"expense\"'>AND 1=1</if>" +
            "  UNION ALL " +
            "  SELECT id FROM sq_income WHERE deleted = 0 AND status = 2 AND IFNULL(quoted_price, 0) - IFNULL(actual_income, 0) > 0 " +
            "  <if test='startDate != null'>AND create_time &gt;= #{startDate}</if>" +
            "  <if test='endDate != null'>AND create_time &lt;= #{endDate}</if>" +
            "  <if test='direction != null and direction == \"income\"'>AND 1=1</if>" +
            ") all_count" +
            "</script>")
    long countAllFlow(
        @Param("startDate") String startDate,
        @Param("endDate") String endDate,
        @Param("direction") String direction
    );
}
