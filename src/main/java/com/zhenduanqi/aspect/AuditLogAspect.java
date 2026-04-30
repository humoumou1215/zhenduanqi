package com.zhenduanqi.aspect;

import com.zhenduanqi.annotation.AuditLog;
import com.zhenduanqi.entity.SysAuditLog;
import com.zhenduanqi.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

@Aspect
@Component
public class AuditLogAspect {

    private final AuditLogRepository auditLogRepository;

    public AuditLogAspect(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Around("@annotation(com.zhenduanqi.annotation.AuditLog)")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        AuditLog annotation = signature.getMethod().getAnnotation(AuditLog.class);

        SysAuditLog log = new SysAuditLog();
        log.setAction(annotation.action());

        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest req = attrs.getRequest();
            log.setUsername((String) req.getAttribute("username"));
            log.setUserIp(req.getRemoteAddr());

            Object[] args = joinPoint.getArgs();
            if (args != null) {
                String[] stringArgs = Arrays.stream(args)
                        .filter(a -> a instanceof String)
                        .toArray(String[]::new);
                if (stringArgs.length > 0) log.setCommand(stringArgs[0]);
                if (stringArgs.length > 1) log.setTarget(stringArgs[1]);

                String masked = Arrays.toString(args);
                for (String field : annotation.maskFields()) {
                    masked = masked.replaceAll(field + "=\\S+", field + "=******");
                }
                log.setParams(masked);
            }
        }

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            log.setResult("SUCCESS");
            log.setResultDetail(result != null ? result.toString() : null);
            return result;
        } catch (Exception e) {
            log.setResult("FAILED");
            log.setResultDetail(e.getMessage());
            throw e;
        } finally {
            log.setDurationMs(System.currentTimeMillis() - start);
            auditLogRepository.save(log);
        }
    }
}
