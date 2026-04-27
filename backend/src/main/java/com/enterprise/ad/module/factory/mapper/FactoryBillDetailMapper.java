package com.enterprise.ad.module.factory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.ad.module.factory.entity.FactoryBillDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface FactoryBillDetailMapper extends BaseMapper<FactoryBillDetail> {

    /** 根据账单ID查询明细列表（按登记日期升序） */
    @Select("SELECT * FROM fac_factory_bill_detail " +
            "WHERE bill_id = #{billId} AND deleted = 0 " +
            "ORDER BY record_date ASC, id ASC")
    List<FactoryBillDetail> selectByBillId(@Param("billId") Long billId);

    /** 根据账单ID删除所有明细（逻辑删除） */
    @Select("UPDATE fac_factory_bill_detail SET deleted = 1, update_time = NOW() " +
            "WHERE bill_id = #{billId}")
    void logicDeleteByBillId(@Param("billId") Long billId);

    /** 逻辑删除单条明细 */
    @Update("UPDATE fac_factory_bill_detail SET deleted = 1, update_time = NOW() WHERE id = #{id}")
    void deleteDetailLogical(@Param("id") Long id);
}
