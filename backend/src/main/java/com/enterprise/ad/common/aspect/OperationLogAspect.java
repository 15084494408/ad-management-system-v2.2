package com.enterprise.ad.common.aspect;

import com.enterprise.ad.common.annotation.OperationLog;
import com.enterprise.ad.common.util.WebUtil;
import com.enterprise.ad.module.system.log.service.OperationLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 操作日志 AOP 切面
 * 拦截标注了 @OperationLog 的 Controller 方法，异步写入数据库
 *
 * 使用方式：在 Controller 方法上添加 @OperationLog 注解
 * @OperationLog(value = "登记收款", module = "订单管理")
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        String module = operationLog.module();
        String operation = operationLog.value();
        String method = joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName();

        // 获取请求信息
        String ip = "unknown";
        String username = "anonymous";
        Long userId = null;
        String url = "";
        String userAgent = "";

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            ip = WebUtil.getClientIp(request);
            url = request.getRequestURI();
            userAgent = request.getHeader("User-Agent");
            if (request.getAttribute("username") != null) {
                username = request.getAttribute("username").toString();
            }
            if (request.getAttribute("userId") != null) {
                userId = Long.valueOf(request.getAttribute("userId").toString());
            }
        }

        // 记录请求参数
        String params = "";
        if (operationLog.recordParams()) {
            params = serializeArgs(joinPoint);
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
            boolean success = exception == null;

            log.info("[OperationLog] module={}, operation={}, user={}, ip={}, cost={}ms, status={}",
                module, operation, username, ip, cost, success ? "SUCCESS" : "FAIL");

            // 异步写入数据库
            saveLogAsync(module, operation, method, url, params, userId, username, ip, userAgent, success, cost);
        }
    }

    /** 序列化方法参数 */
    private String serializeArgs(ProceedingJoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            StringBuilder sb = new StringBuilder("[");
            int count = 0;
            for (Object arg : args) {
                if (arg instanceof HttpServletRequest || arg instanceof jakarta.servlet.http.HttpServletResponse) {
                    continue;
                }
                if (count > 0) sb.append(", ");
                try {
                    sb.append(objectMapper.writeValueAsString(arg));
                } catch (Exception e) {
                    sb.append(arg != null ? arg.toString() : "null");
                }
                count++;
            }
            sb.append("]");
            String params = sb.toString();
            return params.length() > 2000 ? params.substring(0, 2000) + "...(truncated)" : params;
        } catch (Exception e) {
            return "[failed to serialize]";
        }
    }

    /** 异步写入数据库，不影响主流程 */
    private void saveLogAsync(String module, String operation, String method, String url,
                             String params, Long userId, String username, String ip,
                             String userAgent, boolean success, long cost) {
        try {
            com.enterprise.ad.module.system.log.entity.OperationLog logEntity = new com.enterprise.ad.module.system.log.entity.OperationLog();
            logEntity.setModule(module);
            logEntity.setAction(operation);
            logEntity.setDescription(operation);
            logEntity.setMethod(method);
            logEntity.setUrl(url);
            logEntity.setParam(params);
            logEntity.setUserId(userId);
            logEntity.setUsername(username);
            logEntity.setIp(ip);
            logEntity.setUserAgent(userAgent != null && userAgent.length() > 500 ? userAgent.substring(0, 500) : userAgent);
            logEntity.setStatus(success ? 1 : 0);
            logEntity.setDuration(cost);
            // 异步写入，不影响主流程性能
            CompletableFuture.runAsync(() -> {
                try {
                    operationLogService.save(logEntity);
                } catch (Exception e) {
                    log.error("[OperationLog] 写入数据库失败: {}", e.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("[OperationLog] 构建日志失败: {}", e.getMessage());
        }
    }
}
