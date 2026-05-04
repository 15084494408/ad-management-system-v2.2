package com.enterprise.ad.module.factory.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import com.enterprise.ad.module.factory.service.CustomerService;
import com.enterprise.ad.module.customer.mapper.CustomerMapper;
import com.enterprise.ad.module.customer.entity.Customer;

/**
 * Customer 服务实现
 */
@Service
public class CustomerServiceImpl extends ServiceImpl<CustomerMapper, Customer> implements CustomerService {

}