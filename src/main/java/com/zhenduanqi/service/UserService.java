package com.zhenduanqi.service;

import com.zhenduanqi.dto.CreateUserRequest;
import com.zhenduanqi.dto.UserResponse;
import com.zhenduanqi.entity.SysRole;
import com.zhenduanqi.entity.SysUser;
import com.zhenduanqi.repository.SysRoleRepository;
import com.zhenduanqi.repository.SysUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final SysUserRepository userRepository;
    private final SysRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(SysUserRepository userRepository, SysRoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public java.util.List<UserResponse> list() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public UserResponse create(CreateUserRequest req) {
        if (userRepository.findByUsername(req.getUsername()).isPresent()) {
            throw new RuntimeException("用户名已存在");
        }
        if (req.getPassword() == null || req.getPassword().length() < 8) {
            throw new RuntimeException("密码至少8位");
        }

        SysUser user = new SysUser();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRealName(req.getRealName());
        user.setStatus("ACTIVE");

        if (req.getRoleCodes() != null && !req.getRoleCodes().isEmpty()) {
            Set<SysRole> roles = req.getRoleCodes().stream()
                    .map(code -> roleRepository.findByRoleCode(code)
                            .orElseThrow(() -> new RuntimeException("角色不存在: " + code)))
                    .collect(Collectors.toSet());
            user.setRoles(roles);
        }

        SysUser saved = userRepository.save(user);
        return toResponse(saved);
    }

    public UserResponse update(Long id, com.zhenduanqi.dto.UpdateUserRequest req) {
        SysUser user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (req.getRealName() != null) user.setRealName(req.getRealName());
        if (req.getStatus() != null) user.setStatus(req.getStatus());
        if (req.getRoleCodes() != null) {
            Set<SysRole> roles = req.getRoleCodes().stream()
                    .map(code -> roleRepository.findByRoleCode(code)
                            .orElseThrow(() -> new RuntimeException("角色不存在: " + code)))
                    .collect(Collectors.toSet());
            user.setRoles(roles);
        }
        return toResponse(userRepository.save(user));
    }

    public void resetPassword(Long id, String newPassword) {
        if (newPassword == null || newPassword.length() < 8) {
            throw new RuntimeException("密码至少8位");
        }
        SysUser user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private UserResponse toResponse(SysUser user) {
        UserResponse resp = new UserResponse();
        resp.setId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setRealName(user.getRealName());
        resp.setStatus(user.getStatus());
        resp.setFailCount(user.getFailCount());
        resp.setCreatedAt(user.getCreatedAt());
        if (user.getRoles() != null) {
            resp.setRoles(user.getRoles().stream()
                    .map(r -> new UserResponse.RoleInfo(r.getRoleCode(), r.getRoleName()))
                    .collect(Collectors.toList()));
        }
        return resp;
    }
}
