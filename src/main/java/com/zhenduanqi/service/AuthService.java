package com.zhenduanqi.service;

import com.zhenduanqi.config.JwtUtil;
import com.zhenduanqi.config.LoginRateLimiter;
import com.zhenduanqi.config.TokenBlacklist;
import com.zhenduanqi.dto.LoginResponse;
import com.zhenduanqi.entity.SysUser;
import com.zhenduanqi.repository.SysUserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final SysUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenBlacklist tokenBlacklist;
    private final LoginRateLimiter rateLimiter;

    public AuthService(SysUserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       TokenBlacklist tokenBlacklist,
                       LoginRateLimiter rateLimiter) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.tokenBlacklist = tokenBlacklist;
        this.rateLimiter = rateLimiter;
    }

    public LoginResponse login(String username, String password, String ip, HttpServletResponse response) {
        final String trimmedUsername = username != null ? username.trim() : null;
        final String trimmedPassword = password != null ? password.trim() : null;

        if (rateLimiter.isBlocked(ip)) {
            log.info("登录失败: IP限流, username={}, ip={}", trimmedUsername, ip);
            throw new RuntimeException("登录尝试过于频繁，请稍后再试");
        }

        SysUser user = userRepository.findByUsername(trimmedUsername)
                .orElseThrow(() -> {
                    rateLimiter.recordFailure(ip);
                    log.info("登录失败: 用户不存在, username={}, ip={}", trimmedUsername, ip);
                    return new RuntimeException("用户名或密码错误");
                });

        if (!"ACTIVE".equals(user.getStatus())) {
            log.info("登录失败: 账户已禁用, username={}", trimmedUsername);
            throw new RuntimeException("账户已被禁用");
        }

        if (user.getLockUntil() != null && LocalDateTime.now().isBefore(user.getLockUntil())) {
            log.info("登录失败: 账户已锁定, username={}, lockUntil={}", trimmedUsername, user.getLockUntil());
            throw new RuntimeException("账户已被锁定，请在 " + user.getLockUntil() + " 后重试");
        }

        if (!passwordEncoder.matches(trimmedPassword, user.getPassword())) {
            rateLimiter.recordFailure(ip);
            user.setFailCount(user.getFailCount() + 1);
            if (user.getFailCount() >= 5) {
                user.setLockUntil(LocalDateTime.now().plusMinutes(15));
                user.setFailCount(0);
                log.warn("账户锁定: username={}, ip={}", trimmedUsername, ip);
            } else {
                log.info("登录失败: 密码错误, username={}, ip={}, failCount={}", trimmedUsername, ip, user.getFailCount());
            }
            userRepository.save(user);
            throw new RuntimeException("用户名或密码错误");
        }

        user.setFailCount(0);
        user.setLockUntil(null);
        userRepository.save(user);
        rateLimiter.reset(ip);

        String token = jwtUtil.generateToken(trimmedUsername);
        Cookie cookie = new Cookie("zhenduanqi_token", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/api");
        cookie.setMaxAge(7200);
        response.addCookie(cookie);

        log.info("登录成功: username={}, ip={}", trimmedUsername, ip);
        return new LoginResponse(trimmedUsername, getUserRole(trimmedUsername), user.getRealName());
    }

    public void logout(String token, HttpServletResponse response) {
        if (token != null && !token.isEmpty()) {
            tokenBlacklist.add(token, 7200_000L);
        }
        Cookie cookie = new Cookie("zhenduanqi_token", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/api");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        log.info("登出成功");
    }

    public String validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        if (tokenBlacklist.contains(token)) {
            return null;
        }
        try {
            return jwtUtil.validateAndGetUsername(token);
        } catch (Exception e) {
            return null;
        }
    }

    public String getUserRole(String username) {
        return userRepository.findByUsername(username)
                .map(user -> {
                    if (user.getRoles() == null || user.getRoles().isEmpty()) {
                        return "READONLY";
                    }
                    return user.getRoles().iterator().next().getRoleCode();
                })
                .orElse("READONLY");
    }
}
