package com.zhenduanqi.aspect;

import com.zhenduanqi.annotation.AuditLog;
import com.zhenduanqi.dto.ExecuteResponse;
import com.zhenduanqi.entity.SysAuditLog;
import com.zhenduanqi.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

@Aspect
@Component
public class AuditLogAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditLogAspect.class);

    private final AuditLogRepository auditLogRepository;

    public AuditLogAspect(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Around("@annotation(com.zhenduanqi.annotation.AuditLog)")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        AuditLog annotation = signature.getMethod().getAnnotation(AuditLog.class);

        SysAuditLog auditEntry = new SysAuditLog();
        auditEntry.setAction(annotation.action());

        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest req = attrs.getRequest();
            auditEntry.setUsername((String) req.getAttribute("username"));
            auditEntry.setUserIp(req.getRemoteAddr());

            Object[] args = joinPoint.getArgs();
            if (args != null) {
                for (Object arg : args) {
                    if (arg instanceof String) {
                        if (auditEntry.getCommand() == null) {
                            auditEntry.setCommand((String) arg);
                        } else if (auditEntry.getTarget() == null) {
                            auditEntry.setTarget((String) arg);
                        }
                    } else if (arg != null) {
                        String argStr = arg.toString();
                        if (argStr.contains("serverId")) {
                            try {
                                String serverId = extractField(argStr, "serverId");
                                String command = extractField(argStr, "command");
                                if (serverId != null) auditEntry.setTarget(serverId);
                                if (command != null) auditEntry.setCommand(command);
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
                auditEntry.setParams(masked);
            }
        }

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            ExecuteResponse execResp = extractExecuteResponse(result);
            if (execResp != null) {
                if ("blocked".equals(execResp.getState())) {
                    auditEntry.setResult("BLOCKED");
                } else if (!"succeeded".equals(execResp.getState())) {
                    auditEntry.setResult("FAILED");
                } else {
                    auditEntry.setResult("SUCCESS");
                }
            } else {
                auditEntry.setResult("SUCCESS");
            }
            auditEntry.setResultDetail(result != null ? result.toString() : null);
            log.debug("审计日志写入成功: action={}, user={}, result={}", auditEntry.getAction(), auditEntry.getUsername(), auditEntry.getResult());
            return result;
        } catch (Exception e) {
            auditEntry.setResult("FAILED");
            auditEntry.setResultDetail(e.getMessage());
            log.error("审计日志写入失败: action={}, user={}, error={}", auditEntry.getAction(), auditEntry.getUsername(), e.getMessage());
            throw e;
        } finally {
            auditEntry.setDurationMs(System.currentTimeMillis() - start);
            auditLogRepository.save(auditEntry);
        }
    }

    private ExecuteResponse extractExecuteResponse(Object result) {
        if (result instanceof ResponseEntity<?> responseEntity) {
            Object body = responseEntity.getBody();
            if (body instanceof ExecuteResponse execResp) {
                return execResp;
            }
        }
        if (result instanceof ExecuteResponse execResp) {
            return execResp;
        }
        return null;
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
