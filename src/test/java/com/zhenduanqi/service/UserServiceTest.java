package com.zhenduanqi.service;

import com.zhenduanqi.dto.CreateUserRequest;
import com.zhenduanqi.dto.UpdateUserRequest;
import com.zhenduanqi.dto.UserResponse;
import com.zhenduanqi.entity.SysRole;
import com.zhenduanqi.entity.SysUser;
import com.zhenduanqi.repository.SysRoleRepository;
import com.zhenduanqi.repository.SysUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private SysUserRepository userRepository;
    @Mock
    private SysRoleRepository roleRepository;

    private PasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        userService = new UserService(userRepository, roleRepository, passwordEncoder);
    }

    @Test
    void create_withValidData_returnsUserResponse() {
        SysRole adminRole = new SysRole();
        adminRole.setId(1L);
        adminRole.setRoleCode("ADMIN");
        adminRole.setRoleName("管理员");

        when(roleRepository.findByRoleCode("ADMIN")).thenReturn(Optional.of(adminRole));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CreateUserRequest req = new CreateUserRequest();
        req.setUsername("newuser");
        req.setPassword("Abcd1234");
        req.setRealName("新用户");
        req.setRoleCodes(List.of("ADMIN"));

        UserResponse resp = userService.create(req);

        assertThat(resp.getUsername()).isEqualTo("newuser");
        assertThat(resp.getRealName()).isEqualTo("新用户");
        assertThat(resp.getRoles()).hasSize(1);
        assertThat(resp.getRoles().get(0).getCode()).isEqualTo("ADMIN");
        verify(userRepository).save(any());
    }

    @Test
    void create_withDuplicateUsername_throwsException() {
        when(userRepository.findByUsername("existing")).thenReturn(Optional.of(new SysUser()));

        CreateUserRequest req = new CreateUserRequest();
        req.setUsername("existing");
        req.setPassword("Abcd1234");

        assertThatThrownBy(() -> userService.create(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("用户名已存在");
    }

    @Test
    void create_withShortPassword_throwsException() {
        CreateUserRequest req = new CreateUserRequest();
        req.setUsername("newuser");
        req.setPassword("1234567");

        assertThatThrownBy(() -> userService.create(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("密码至少8位");
    }
}
