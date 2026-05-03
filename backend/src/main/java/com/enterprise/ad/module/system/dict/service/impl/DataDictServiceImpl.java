package com.enterprise.ad.module.system.dict.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import com.enterprise.ad.module.system.dict.service.DataDictService;
import com.enterprise.ad.module.system.dict.mapper.DataDictMapper;
import com.enterprise.ad.module.system.dict.entity.DataDict;

@Service
public class DataDictServiceImpl extends ServiceImpl<DataDictMapper, DataDict> implements DataDictService {}
