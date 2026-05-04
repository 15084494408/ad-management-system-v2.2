package com.enterprise.ad.module.factory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.ad.module.factory.entity.FactoryBill;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface FactoryBillMapper extends BaseMapper<FactoryBill> {

    /** 逻辑删除账单（原生 SQL，绕过 MyBatis-Plus updateById 忽略 deleted 字段的问题） */
    @Update("UPDATE fac_factory_bill SET deleted = 1, update_time = NOW() WHERE id = #{id}")
    void deleteBillLogical(@Param("id") Long id);

    // ★ 修复 P1-11: 应付聚合查询
    @Select("SELECT COALESCE(SUM(total_amount), 0) FROM fac_factory_bill WHERE deleted = 0")
    BigDecimal sumAllTotalAmount();

    @Select("SELECT COALESCE(SUM(paid_amount), 0) FROM fac_factory_bill WHERE deleted = 0")
    BigDecimal sumAllPaidAmount();

    /** 检查指定客户+月份是否已存在客户账单（billType=2） */
    @Select("SELECT COUNT(*) FROM fac_factory_bill " +
            "WHERE deleted = 0 AND bill_type = 2 " +
            "AND customer_id = #{customerId} AND month = #{month}")
    int countByCustomerAndMonth(@Param("customerId") Long customerId, @Param("month") String month);
}
