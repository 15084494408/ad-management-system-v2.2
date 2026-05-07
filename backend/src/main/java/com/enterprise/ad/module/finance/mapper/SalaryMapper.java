package com.enterprise.ad.module.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.ad.module.finance.entity.Salary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface SalaryMapper extends BaseMapper<Salary> {

    /** 查询指定月份某员工的已结算提成总额 */
    @Select("SELECT COALESCE(SUM(commission_amount), 0) FROM fin_designer_commission " +
            "WHERE designer_id = #{employeeId} AND status >= 2 AND deleted = 0 " +
            "AND DATE_FORMAT(settle_time, '%Y-%m') = #{month}")
    BigDecimal sumCommissionByMonth(Long employeeId, String month);

    /** 查询所有员工用户 */
    @Select("SELECT id, username, real_name FROM sys_user WHERE deleted = 0 AND status = 1 ORDER BY id")
    List<Map<String, Object>> selectAllEmployees();
}
