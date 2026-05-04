package com.enterprise.ad.module.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.ad.module.customer.entity.CustomerLevel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CustomerLevelMapper extends BaseMapper<CustomerLevel> {

    @Select("SELECT cl.*, COUNT(c.id) AS customerCount FROM customer_level cl " +
            "LEFT JOIN crm_customer c ON c.level = cl.level AND c.deleted = 0 " +
            "WHERE cl.deleted = 0 GROUP BY cl.id ORDER BY cl.level DESC")
    java.util.List<java.util.Map<String, Object>> listWithCustomerCount();
}
