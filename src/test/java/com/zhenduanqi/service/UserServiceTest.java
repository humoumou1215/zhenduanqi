package com.zhenduanqi.service;

import com.zhenduanqi.dto.CreateUserRequest;
import com.zhenduanqi.dto.UserResponse;
import com.zhenduanqi.entity.SysRole;
import com.zhenduanqi.repository.SysRoleRepository;
import com.zhenduanqi.repository.SysUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

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

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private UserService userService;

    @Test
    void create_withValidData_returnsUserResponse() {
        userService = new UserService(userRepository, roleRepository, passwordEncoder);

        SysRole role = new SysRole();
        role.setRoleCode("ADMIN");
        role.setRoleName("管理员");
        when(roleRepository.findByRoleCode("ADMIN")).thenReturn(Optional.of(role));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CreateUserRequest req = new CreateUserRequest();
        req.setUsername("zhangsan");
        req.setPassword("Abcd1234");
        req.setRealName("张三");
        req.setRoleCodes(List.of("ADMIN"));

        UserResponse resp = userService.create(req);

        assertThat(resp.getUsername()).isEqualTo("zhangsan");
        assertThat(resp.getRealName()).isEqualTo("张三");
        assertThat(resp.getRoles()).hasSize(1);
        assertThat(resp.getRoles().get(0).getCode()).isEqualTo("ADMIN");
        verify(userRepository).save(any());
    }

    @Test
    void create_withDuplicateUsername_throwsException() {
        userService = new UserService(userRepository, roleRepository, passwordEncoder);

        when(userRepository.findByUsername("zhangsan")).thenReturn(Optional.of(new com.zhenduanqi.entity.SysUser()));

        CreateUserRequest req = new CreateUserRequest();
        req.setUsername("zhangsan");
        req.setPassword("Abcd1234");

        assertThatThrownBy(() -> userService.create(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("用户名已存在");
    }

    @Test
    void create_withShortPassword_throwsException() {
        userService = new UserService(userRepository, roleRepository, passwordEncoder);

        CreateUserRequest req = new CreateUserRequest();
        req.setUsername("zhangsan");
        req.setPassword("1234567");

        assertThatThrownBy(() -> userService.create(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("密码至少8位");
    }
}
