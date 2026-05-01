package com.zhenduanqi.aspect;

import com.zhenduanqi.annotation.RequireRole;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.Set;

@Aspect
@Component
public class RoleAspect {

    private static final Logger log = LoggerFactory.getLogger(RoleAspect.class);

    @Around("@annotation(com.zhenduanqi.annotation.RequireRole)")
    public Object checkRole(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        RequireRole annotation = signature.getMethod().getAnnotation(RequireRole.class);
        String[] allowedRoles = annotation.value();

        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new RuntimeException("无请求上下文");
        }

        String username = (String) attrs.getRequest().getAttribute("username");
        if (username == null) {
            attrs.getResponse().setStatus(401);
            attrs.getResponse().getWriter().write("{\"error\":\"未登录\"}");
            return null;
        }

        // 直接从 request attribute 读取角色集合
        @SuppressWarnings("unchecked")
        Set<String> userRoles = (Set<String>) attrs.getRequest().getAttribute("userRoles");
        if (userRoles == null) {
            attrs.getResponse().setStatus(403);
            attrs.getResponse().getWriter().write("{\"error\":\"用户不存在\"}");
            return null;
        }

        boolean hasRole = Arrays.stream(allowedRoles).anyMatch(userRoles::contains);
        if (!hasRole) {
            log.warn("权限不足: username={}, userRoles={}, requiredRoles={}", username, userRoles, Arrays.toString(allowedRoles));
            attrs.getResponse().setStatus(403);
            attrs.getResponse().getWriter().write("{\"error\":\"权限不足\"}");
            return null;
        }

        return joinPoint.proceed();
    }
}
