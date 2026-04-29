package com.enterprise.ad.common.aspect;

import com.enterprise.ad.common.annotation.OperationLog;
import com.enterprise.ad.common.util.WebUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * ★ 修复 P3-21: 操作日志 AOP 切面
 * 自动记录关键业务操作日志（如收款、删除订单等）
 *
 * 使用方式：在 Controller 方法上添加 @OperationLog 注解
 * @OperationLog(value = "登记收款", module = "订单管理")
 */
@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        String module = operationLog.module();
        String operation = operationLog.value();

        // 获取请求信息
        String ip = "unknown";
        String username = "anonymous";
        String method = joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName();

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            ip = WebUtil.getClientIp(request);
            username = request.getAttribute("username") != null ? request.getAttribute("username").toString() : username;
        }

        // 记录请求参数
        String params = "";
        if (operationLog.recordParams()) {
            try {
                Object[] args = joinPoint.getArgs();
                // 过滤掉 HttpServletRequest/Response 等不可序列化的参数
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < args.length; i++) {
                    if (args[i] instanceof HttpServletRequest || args[i] instanceof jakarta.servlet.http.HttpServletResponse) {
                        continue;
                    }
                    if (i > 0) sb.append(", ");
                    try {
                        sb.append(objectMapper.writeValueAsString(args[i]));
                    } catch (Exception e) {
                        sb.append(args[i] != null ? args[i].toString() : "null");
                    }
                }
                sb.append("]");
                params = sb.toString();
                // 截断过长的参数
                if (params.length() > 2000) {
                    params = params.substring(0, 2000) + "...(truncated)";
                }
            } catch (Exception e) {
                params = "[failed to serialize]";
            }
        }

        // 执行目标方法
        Object result;
        Throwable exception = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            exception = e;
            throw e;
        } finally {
            long cost = System.currentTimeMillis() - startTime;
            String status = exception != null ? "FAIL" : "SUCCESS";

            log.info("[OperationLog] module={}, operation={}, user={}, ip={}, method={}, cost={}ms, status={}, params={}",
                module, operation, username, ip, method, cost, status, params);

            if (exception != null) {
                log.warn("[OperationLog] Exception: {}", exception.getMessage());
            }
        }
    }
}
