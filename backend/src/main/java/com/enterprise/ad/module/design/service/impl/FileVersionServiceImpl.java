package com.enterprise.ad.module.design.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import com.enterprise.ad.module.design.service.FileVersionService;
import com.enterprise.ad.module.design.mapper.FileVersionMapper;
import com.enterprise.ad.module.design.entity.FileVersion;

/**
 * FileVersion 服务实现
 */
@Service
public class FileVersionServiceImpl extends ServiceImpl<FileVersionMapper, FileVersion> implements FileVersionService {

}