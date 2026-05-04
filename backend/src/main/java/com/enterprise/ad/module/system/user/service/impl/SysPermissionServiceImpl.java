package com.enterprise.ad.module.system.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import com.enterprise.ad.module.system.user.service.SysPermissionService;
import com.enterprise.ad.module.system.user.mapper.SysPermissionMapper;
import com.enterprise.ad.module.system.user.entity.SysPermission;

/**
 * SysPermission 服务实现
 */
@Service
public class SysPermissionServiceImpl extends ServiceImpl<SysPermissionMapper, SysPermission> implements SysPermissionService {

}