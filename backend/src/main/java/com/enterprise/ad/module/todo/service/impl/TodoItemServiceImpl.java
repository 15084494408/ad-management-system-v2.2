package com.enterprise.ad.module.todo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import com.enterprise.ad.module.todo.service.TodoItemService;
import com.enterprise.ad.module.todo.mapper.TodoItemMapper;
import com.enterprise.ad.module.todo.entity.TodoItem;

/**
 * TodoItem 服务实现
 */
@Service
public class TodoItemServiceImpl extends ServiceImpl<TodoItemMapper, TodoItem> implements TodoItemService {

}
