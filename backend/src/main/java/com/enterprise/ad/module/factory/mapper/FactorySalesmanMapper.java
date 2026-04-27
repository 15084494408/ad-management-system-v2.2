package com.enterprise.ad.module.factory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.ad.module.factory.entity.FactorySalesman;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface FactorySalesmanMapper extends BaseMapper<FactorySalesman> {

    /**
     * 按工厂ID查询业务员列表
     */
    List<FactorySalesman> selectByFactoryId(@Param("factoryId") Long factoryId);

    /**
     * 按工厂ID查询启用的业务员列表
     */
    List<FactorySalesman> selectEnabledByFactoryId(@Param("factoryId") Long factoryId);
}
