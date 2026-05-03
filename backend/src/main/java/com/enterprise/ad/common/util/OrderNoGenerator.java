package com.enterprise.ad.common.util;

import com.enterprise.ad.module.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 订单编号生成器 — 统一使用Redis INCR原子自增
 * <p>
 * 格式: DD + yyyyMMdd + 3位序号 (如 DD20260503001)
 * Redis不可用时降级为数据库查询
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderNoGenerator {

    private static final String ORDER_PREFIX = "DD";
    private static final String REDIS_KEY_PREFIX = "order:no:seq:";

    private final StringRedisTemplate redisTemplate;
    private final OrderMapper orderMapper;

    /**
     * 生成订单编号
     */
    public String generate() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String redisKey = REDIS_KEY_PREFIX + dateStr;
        String prefix = ORDER_PREFIX + dateStr;

        Long seq = redisTemplate.opsForValue().increment(redisKey);
        if (seq != null && seq == 1) {
            redisTemplate.expire(redisKey, 24, TimeUnit.HOURS);
        }

        if (seq == null) {
            log.warn("Redis不可用，降级使用数据库查询生成订单号");
            return fallback(prefix);
        }

        return prefix + String.format("%03d", seq);
    }

    /**
     * Redis降级方案：查询数据库最大订单号+1
     */
    private String fallback(String prefix) {
        String maxNo = orderMapper.selectMaxOrderNo(prefix);
        int seq = 1;
        if (maxNo != null && maxNo.length() > prefix.length()) {
            try {
                seq = Integer.parseInt(maxNo.substring(prefix.length())) + 1;
            } catch (NumberFormatException e) {
                log.warn("订单号解析失败: {}, 使用默认序号1", maxNo);
            }
        }
        return prefix + String.format("%03d", seq);
    }
}
