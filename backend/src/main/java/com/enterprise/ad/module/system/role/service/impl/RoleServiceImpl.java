package com.enterprise.ad.module.system.role.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import com.enterprise.ad.module.system.role.service.RoleService;
import com.enterprise.ad.module.system.role.mapper.RoleMapper;
import com.enterprise.ad.module.system.role.entity.Role;

@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {}
