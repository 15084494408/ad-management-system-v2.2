package com.enterprise.ad.module.system.log.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import com.enterprise.ad.module.system.log.service.OperationLogService;
import com.enterprise.ad.module.system.log.mapper.OperationLogMapper;
import com.enterprise.ad.module.system.log.entity.OperationLog;

/**
 * OperationLog 服务实现
 */
@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements OperationLogService {

}