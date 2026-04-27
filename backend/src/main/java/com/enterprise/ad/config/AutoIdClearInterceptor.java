package com.enterprise.ad.config;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

/**
 * MyBatis 全局拦截器
 * INSERT 操作前强制清空实体的 id 字段，防止前端传入已有 id 导致主键冲突（DuplicateKeyException）
 * 配合全局 id-type: auto 使用，让数据库自增生成主键
 */
@Slf4j
@Intercepts({
    @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
})
@Component
public class AutoIdClearInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        Object parameter = invocation.getArgs()[1];

        // 只拦截 INSERT 操作
        if (ms.getSqlCommandType() == SqlCommandType.INSERT && parameter != null) {
            clearId(parameter);
        }

        return invocation.proceed();
    }

    /**
     * 清空实体的 id 字段
     * 支持直接实体对象、@Param 包装、Map 包装等多种参数形式
     */
    private void clearId(Object parameter) {
        Object entity = extractEntity(parameter);
        if (entity == null) return;

        try {
            Field idField = findIdField(entity.getClass());
            if (idField != null) {
                idField.setAccessible(true);
                Object idValue = idField.get(entity);
                if (idValue != null) {
                    idField.set(entity, null);
                    log.debug("AutoIdClearInterceptor: 清空实体 {} 的 id={}", entity.getClass().getSimpleName(), idValue);
                }
            }
        } catch (Exception e) {
            // 清空失败不影响正常流程
            log.trace("AutoIdClearInterceptor: 清空 id 跳过 - {}", e.getMessage());
        }
    }

    /**
     * 从参数中提取实体对象
     * MyBatis 参数可能是：实体本身、ParamMap 包装、或带 @Param 注解的对象
     */
    private Object extractEntity(Object parameter) {
        if (parameter == null) return null;
        // 直接是实体（非 Map）
        if (!(parameter instanceof java.util.Map)) {
            return parameter;
        }
        // ParamMap（MyBatis-Plus 常见的参数包装形式）
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> paramMap = (java.util.Map<String, Object>) parameter;
        // MyBatis-Plus 的 ParamMap 中，实体通常存在 "et" 或 "param1" 或第一个非特殊 key
        if (paramMap.containsKey("et")) {
            return paramMap.get("et");
        }
        if (paramMap.containsKey("param1")) {
            Object p1 = paramMap.get("param1");
            if (p1 != null && !(p1 instanceof java.util.Map)) return p1;
        }
        // 尝试找第一个非默认的值作为实体
        for (java.util.Map.Entry<String, Object> entry : paramMap.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith("param") && !key.equals("ew") && !key.startsWith("mpw")) {
                Object val = entry.getValue();
                if (val != null && !(val instanceof java.util.Map) && !(val instanceof java.util.Collection)) {
                    return val;
                }
            }
        }
        return null;
    }

    /**
     * 查找 id 字段（包括父类）
     */
    private Field findIdField(Class<?> clazz) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if ("id".equals(field.getName())) {
                    return field;
                }
                // 也匹配被注解标记的字段
                if (field.getAnnotation(TableId.class) != null) {
                    return field;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }
}
