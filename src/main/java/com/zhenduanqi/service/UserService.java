package com.zhenduanqi.service;

import com.zhenduanqi.dto.CreateUserRequest;
import com.zhenduanqi.dto.UserResponse;
import com.zhenduanqi.entity.SysRole;
import com.zhenduanqi.entity.SysUser;
import com.zhenduanqi.repository.SysRoleRepository;
import com.zhenduanqi.repository.SysUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).+$");
    private static final int USERNAME_MIN_LENGTH = 2;
    private static final int USERNAME_MAX_LENGTH = 50;
    private static final int REAL_NAME_MAX_LENGTH = 100;

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
        validateUsername(req.getUsername());
        validatePassword(req.getPassword());
        validateRealName(req.getRealName());

        if (userRepository.findByUsername(req.getUsername()).isPresent()) {
            throw new RuntimeException("用户名已存在");
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
        log.info("用户创建: username={}", saved.getUsername());
        return toResponse(saved);
    }

    public UserResponse update(Long id, com.zhenduanqi.dto.UpdateUserRequest req) {
        SysUser user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (req.getRealName() != null) {
            validateRealName(req.getRealName());
            user.setRealName(req.getRealName());
        }
        if (req.getStatus() != null) user.setStatus(req.getStatus());
        if (req.getRoleCodes() != null) {
            Set<SysRole> roles = req.getRoleCodes().stream()
                    .map(code -> roleRepository.findByRoleCode(code)
                            .orElseThrow(() -> new RuntimeException("角色不存在: " + code)))
                    .collect(Collectors.toSet());
            user.setRoles(roles);
        }
        SysUser saved = userRepository.save(user);
        log.info("用户更新: id={}, username={}", id, user.getUsername());
        return toResponse(saved);
    }

    public void resetPassword(Long id, String newPassword) {
        validatePassword(newPassword);
        SysUser user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("密码重置: id={}, username={}", id, user.getUsername());
    }

    private void validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new RuntimeException("用户名不能为空");
        }
        if (username.length() < USERNAME_MIN_LENGTH || username.length() > USERNAME_MAX_LENGTH) {
            throw new RuntimeException("用户名长度需在" + USERNAME_MIN_LENGTH + "-" + USERNAME_MAX_LENGTH + "位之间");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new RuntimeException("密码至少8位");
        }
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new RuntimeException("密码必须包含字母和数字");
        }
    }

    private void validateRealName(String realName) {
        if (realName != null && realName.length() > REAL_NAME_MAX_LENGTH) {
            throw new RuntimeException("真实姓名不能超过" + REAL_NAME_MAX_LENGTH + "位");
        }
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
