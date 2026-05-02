package com.zhenduanqi.aspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Aspect
@Component
public class AuditLogAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditLogAspect.class);
    private static final String MASK_VALUE = "******";
    private static final Pattern FIELD_PATTERN = Pattern.compile("\"(\\w+)\"\\s*:\\s*\"([^\"]*)\"");

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditLogAspect(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
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
            if (args != null && args.length > 0) {
                extractCommandAndTarget(args, auditEntry);
                String maskedParams = serializeWithMasking(args, annotation.maskFields());
                auditEntry.setParams(maskedParams);
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
            try {
                auditLogRepository.save(auditEntry);
            } catch (Exception e) {
                log.error("审计日志保存失败: action={}, user={}, error={}", auditEntry.getAction(), auditEntry.getUsername(), e.getMessage());
            }
        }
    }

    private void extractCommandAndTarget(Object[] args, SysAuditLog auditEntry) {
        for (Object arg : args) {
            if (arg == null) continue;
            
            if (arg instanceof String str) {
                if (auditEntry.getCommand() == null) {
                    auditEntry.setCommand(str);
                } else if (auditEntry.getTarget() == null) {
                    auditEntry.setTarget(str);
                }
            } else {
                try {
                    String json = objectMapper.writeValueAsString(arg);
                    extractFromJson(json, auditEntry);
                } catch (JsonProcessingException e) {
                    log.debug("无法序列化参数: {}", e.getMessage());
                }
            }
        }
    }

    private void extractFromJson(String json, SysAuditLog auditEntry) {
        Matcher matcher = FIELD_PATTERN.matcher(json);
        while (matcher.find()) {
            String fieldName = matcher.group(1);
            String value = matcher.group(2);
            
            if ("command".equals(fieldName) && auditEntry.getCommand() == null) {
                auditEntry.setCommand(value);
            } else if ("serverId".equals(fieldName) && auditEntry.getTarget() == null) {
                auditEntry.setTarget(value);
            } else if ("target".equals(fieldName) && auditEntry.getTarget() == null) {
                auditEntry.setTarget(value);
            } else if ("username".equals(fieldName) && auditEntry.getUsername() == null) {
                auditEntry.setUsername(value);
            }
        }
    }

    private String serializeWithMasking(Object[] args, String[] maskFields) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        
        Set<String> fieldsToMask = new HashSet<>(Arrays.asList(maskFields));
        
        try {
            StringBuilder result = new StringBuilder("[");
            for (int i = 0; i < args.length; i++) {
                if (i > 0) result.append(", ");
                if (args[i] == null) {
                    result.append("null");
                } else if (args[i] instanceof String str) {
                    result.append("\"").append(str).append("\"");
                } else {
                    String json = objectMapper.writeValueAsString(args[i]);
                    json = maskJsonFields(json, fieldsToMask);
                    result.append(json);
                }
            }
            result.append("]");
            return result.toString();
        } catch (JsonProcessingException e) {
            log.debug("序列化参数失败: {}", e.getMessage());
            return Arrays.toString(args);
        }
    }

    private String maskJsonFields(String json, Set<String> fieldsToMask) {
        for (String field : fieldsToMask) {
            json = maskSingleField(json, field);
        }
        return json;
    }

    private String maskSingleField(String json, String fieldName) {
        Pattern pattern = Pattern.compile("(\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*)\"[^\"]*\"");
        Matcher matcher = pattern.matcher(json);
        return matcher.replaceAll("$1\"" + MASK_VALUE + "\"");
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
}
