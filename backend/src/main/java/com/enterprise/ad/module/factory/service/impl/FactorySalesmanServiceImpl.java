package com.enterprise.ad.module.factory.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import com.enterprise.ad.module.factory.service.FactorySalesmanService;
import com.enterprise.ad.module.factory.mapper.FactorySalesmanMapper;
import com.enterprise.ad.module.factory.entity.FactorySalesman;

import java.util.List;

/**
 * FactorySalesman 服务实现
 */
@Service
public class FactorySalesmanServiceImpl extends ServiceImpl<FactorySalesmanMapper, FactorySalesman> implements FactorySalesmanService {

    @Override
    public List<FactorySalesman> selectEnabledByFactoryId(Long factoryId) {
        return baseMapper.selectEnabledByFactoryId(factoryId);
    }
}
