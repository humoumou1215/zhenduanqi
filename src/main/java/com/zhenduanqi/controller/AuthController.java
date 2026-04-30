package com.zhenduanqi.controller;

import com.zhenduanqi.dto.LoginRequest;
import com.zhenduanqi.dto.LoginResponse;
import com.zhenduanqi.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request,
                                   HttpServletRequest servletRequest,
                                   HttpServletResponse servletResponse) {
        try {
            String ip = servletRequest.getRemoteAddr();
            LoginResponse loginResponse = authService.login(
                    request.getUsername(), request.getPassword(), ip, servletResponse);
            return ResponseEntity.ok(loginResponse);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request,
                                       HttpServletResponse response) {
        String token = extractTokenFromCookie(request);
        authService.logout(token, response);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        String username = (String) request.getAttribute("username");
        if (username == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        }
        String role = authService.getUserRole(username);
        return ResponseEntity.ok(new LoginResponse(username, role, null));
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
