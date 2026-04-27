package com.enterprise.ad.module.factory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.ad.module.factory.entity.FactoryBill;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface FactoryBillMapper extends BaseMapper<FactoryBill> {

    /** 逻辑删除账单（原生 SQL，绕过 MyBatis-Plus updateById 忽略 deleted 字段的问题） */
    @Update("UPDATE fac_factory_bill SET deleted = 1, update_time = NOW() WHERE id = #{id}")
    void deleteBillLogical(@Param("id") Long id);
}
