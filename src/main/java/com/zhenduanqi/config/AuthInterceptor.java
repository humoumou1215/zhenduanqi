package com.zhenduanqi.config;

import com.zhenduanqi.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);

    private final AuthService authService;

    public AuthInterceptor(AuthService authService) {
        this.authService = authService;
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
            log.warn("Token无效或缺失: path={}", path);
            response.setStatus(401);
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write("{\"error\":\"未登录或Token已过期\"}");
            return false;
        }

        log.debug("认证通过: username={}, path={}", username, path);
        request.setAttribute("username", username);
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
