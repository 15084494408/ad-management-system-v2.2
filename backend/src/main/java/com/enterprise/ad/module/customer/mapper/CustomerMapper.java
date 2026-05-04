package com.enterprise.ad.module.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.ad.module.customer.entity.Customer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {

    /**
     * 查询系统级零售客户（customer_type=3）
     * 如果不存在则自动创建
     */
    @Select("SELECT * FROM crm_customer WHERE customer_type = 3 AND deleted = 0 LIMIT 1")
    Customer selectRetailCustomer();

    /**
     * 插入零售客户（仅在不存在时调用）
     */
    @Update("INSERT INTO crm_customer (customer_name, customer_type, total_amount, order_count, level, status, " +
            "is_member, balance, total_recharge, total_consume, create_time, update_time, deleted) " +
            "VALUES ('零售客户', 3, 0, 0, 1, 1, 0, 0, 0, 0, NOW(), NOW(), 0)")
    int insertRetailCustomer();

    /**
     * 原子扣减客户会员余额（并发安全）
     * 返回影响行数：0 表示余额不足，1 表示扣减成功
     */
    @Update("UPDATE crm_customer SET balance = balance - #{amount}, " +
            "total_consume = IFNULL(total_consume, 0) + #{amount}, " +
            "update_time = NOW() " +
            "WHERE id = #{customerId} AND is_member = 1 AND balance >= #{amount} AND deleted = 0")
    int deductBalance(@Param("customerId") Long customerId, @Param("amount") BigDecimal amount);

    /**
     * 原子增加客户会员余额（充值）
     */
    @Update("UPDATE crm_customer SET balance = balance + #{amount}, " +
            "total_recharge = IFNULL(total_recharge, 0) + #{amount}, " +
            "update_time = NOW() " +
            "WHERE id = #{customerId} AND is_member = 1 AND deleted = 0")
    int addBalance(@Param("customerId") Long customerId, @Param("amount") BigDecimal amount);

    /**
     * 减少累计消费（退款时调用）
     */
    @Update("UPDATE crm_customer SET " +
            "total_consume = GREATEST(IFNULL(total_consume, 0) - #{amount}, 0), " +
            "update_time = NOW() " +
            "WHERE id = #{customerId} AND is_member = 1 AND deleted = 0")
    int reduceConsume(@Param("customerId") Long customerId, @Param("amount") BigDecimal amount);

    /**
     * 查询会员流水记录，自动关联客户名称
     */
    @Select("<script>" +
            "SELECT t.*, c.customer_name " +
            "FROM mem_member_transaction t " +
            "LEFT JOIN crm_customer c ON t.customer_id = c.id " +
            "WHERE t.deleted = 0 AND t.type = #{type} " +
            "<if test='keyword != null'>" +
            "  AND (t.remark LIKE CONCAT('%', #{keyword}, '%') OR CAST(t.customer_id AS CHAR) LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "ORDER BY t.create_time DESC" +
            "</script>")
    List<Map<String, Object>> selectTransactionsWithName(@Param("type") String type, @Param("keyword") String keyword);
}
