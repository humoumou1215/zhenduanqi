package com.zhenduanqi.controller;

import com.zhenduanqi.annotation.AuditLog;
import com.zhenduanqi.annotation.RequireRole;
import com.zhenduanqi.dto.*;
import com.zhenduanqi.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @RequireRole("ADMIN")
    public List<UserResponse> list() {
        return userService.list();
    }

    @PostMapping
    @RequireRole("ADMIN")
    @AuditLog(action = "创建用户")
    public ResponseEntity<?> create(@RequestBody CreateUserRequest req) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(req));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @RequireRole("ADMIN")
    @AuditLog(action = "更新用户")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody UpdateUserRequest req) {
        try {
            return ResponseEntity.ok(userService.update(id, req));
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            if ("用户不存在".equals(msg)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", msg));
            }
            return ResponseEntity.badRequest().body(Map.of("error", msg));
        }
    }

    @PutMapping("/{id}/reset-password")
    @RequireRole("ADMIN")
    @AuditLog(action = "重置密码")
    public ResponseEntity<?> resetPassword(@PathVariable Long id, @RequestBody ResetPasswordRequest req) {
        try {
            userService.resetPassword(id, req.getNewPassword());
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
