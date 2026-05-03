package com.enterprise.ad.module.customer.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import com.enterprise.ad.module.customer.service.CustomerTagService;
import com.enterprise.ad.module.customer.mapper.CustomerTagMapper;
import com.enterprise.ad.module.customer.entity.CustomerTag;

/**
 * CustomerTag 服务实现
 */
@Service
public class CustomerTagServiceImpl extends ServiceImpl<CustomerTagMapper, CustomerTag> implements CustomerTagService {

}
