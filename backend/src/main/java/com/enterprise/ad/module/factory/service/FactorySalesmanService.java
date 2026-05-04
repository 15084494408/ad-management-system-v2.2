package com.enterprise.ad.module.factory.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.enterprise.ad.module.factory.entity.FactorySalesman;

import java.util.List;

/**
 * FactorySalesman 服务接口
 */
public interface FactorySalesmanService extends IService<FactorySalesman> {

    /**
     * 根据工厂ID查询已启用的业务员列表
     */
    List<FactorySalesman> selectEnabledByFactoryId(Long factoryId);
}
