package com.zhenduanqi.config;

import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.UUID;

public class MdcFilter implements Filter {

    private final JwtUtil jwtUtil;

    public MdcFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        try {
            MDC.put("requestId", UUID.randomUUID().toString());
            MDC.put("username", extractUsername(req));
            MDC.put("clientIp", extractClientIp(req));
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private String extractUsername(HttpServletRequest request) {
        String token = extractTokenFromCookie(request);
        if (token != null) {
            try {
                return jwtUtil.validateAndGetUsername(token);
            } catch (Exception e) {
                return "-";
            }
        }
        return "-";
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

    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.isEmpty()) {
            return xri;
        }
        return request.getRemoteAddr();
    }
}
