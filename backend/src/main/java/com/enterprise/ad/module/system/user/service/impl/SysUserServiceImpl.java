package com.enterprise.ad.module.system.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import com.enterprise.ad.module.system.user.service.SysUserService;
import com.enterprise.ad.module.system.user.mapper.SysUserMapper;
import com.enterprise.ad.module.system.user.entity.SysUser;

/**
 * SysUser 服务实现
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

}