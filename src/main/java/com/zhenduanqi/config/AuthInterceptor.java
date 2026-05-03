package com.zhenduanqi.config;

import com.zhenduanqi.entity.SysRole;
import com.zhenduanqi.entity.SysUser;
import com.zhenduanqi.repository.SysUserRepository;
import com.zhenduanqi.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);

    private final AuthService authService;
    private final SysUserRepository userRepository;

    public AuthInterceptor(AuthService authService, SysUserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();

        if (path.equals("/api/auth/login")) {
            return true;
        }

        String token = extractTokenFromCookie(request);
        String username = authService.validateToken(token);

        if (username == null) {
            if (token == null || token.isEmpty()) {
                log.warn("Token缺失: path={}", path);
            } else if (authService.isTokenBlacklisted(token)) {
                log.warn("Token已吊销: path={}", path);
            } else {
                log.warn("Token无效: path={}", path);
            }
            response.setStatus(401);
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write("{\"error\":\"未登录或Token已过期\"}");
            return false;
        }

        SysUser user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            log.warn("用户不存在: username={}, path={}", username, path);
            response.setStatus(403);
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write("{\"error\":\"用户不存在\"}");
            return false;
        }

        Set<String> userRoles = user.getRoles().stream()
                .map(SysRole::getRoleCode)
                .collect(Collectors.toSet());

        log.debug("认证通过: username={}, roles={}, path={}", username, userRoles, path);
        request.setAttribute("username", username);
        request.setAttribute("userRoles", userRoles);
        return true;
    }

    private String extractTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("zhenduanqi_token".equals(c.getName())) {
                    return c.getValue();
                }
            }
        }
        return null;
    }
}
