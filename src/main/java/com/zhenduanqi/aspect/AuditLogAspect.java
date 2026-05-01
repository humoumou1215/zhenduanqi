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
                for (Object arg : args) {
                    if (arg instanceof String) {
                        if (log.getCommand() == null) {
                            log.setCommand((String) arg);
                        } else if (log.getTarget() == null) {
                            log.setTarget((String) arg);
                        }
                    } else if (arg != null) {
                        String argStr = arg.toString();
                        if (argStr.contains("serverId")) {
                            try {
                                String serverId = extractField(argStr, "serverId");
                                String command = extractField(argStr, "command");
                                if (serverId != null) log.setTarget(serverId);
                                if (command != null) log.setCommand(command);
                            } catch (Exception e) {
                                // ignore parse errors
                            }
                        }
                    }
                }

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

    private String extractField(String str, String fieldName) {
        String pattern = fieldName + "=";
        int start = str.indexOf(pattern);
        if (start < 0) return null;
        start += pattern.length();
        
        if (start < str.length() && str.charAt(start) == '"') {
            int end = str.indexOf('"', start + 1);
            if (end > start) {
                return str.substring(start + 1, end);
            }
        } else {
            int end = start;
            while (end < str.length() && str.charAt(end) != ',' && str.charAt(end) != '}') {
                end++;
            }
            if (end > start) {
                return str.substring(start, end).trim();
            }
        }
        return null;
    }
}
