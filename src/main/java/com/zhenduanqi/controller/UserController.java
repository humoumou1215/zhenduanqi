package com.zhenduanqi.controller;

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
    public ResponseEntity<?> create(@RequestBody CreateUserRequest req) {
        try {
            UserResponse resp = userService.create(req);
            return ResponseEntity.status(HttpStatus.CREATED).body(resp);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @RequireRole("ADMIN")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody UpdateUserRequest req) {
        try {
            UserResponse resp = userService.update(id, req);
            return ResponseEntity.ok(resp);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/reset-password")
    @RequireRole("ADMIN")
    public ResponseEntity<?> resetPassword(@PathVariable Long id, @RequestBody ResetPasswordRequest req) {
        try {
            userService.resetPassword(id, req.getNewPassword());
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
