package com.zhenduanqi.service;

import com.zhenduanqi.config.JwtUtil;
import com.zhenduanqi.config.LoginRateLimiter;
import com.zhenduanqi.config.TokenBlacklist;
import com.zhenduanqi.dto.LoginResponse;
import com.zhenduanqi.entity.SysRole;
import com.zhenduanqi.entity.SysUser;
import com.zhenduanqi.repository.SysUserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private SysUserRepository userRepository;
    @Mock
    private HttpServletResponse response;

    private PasswordEncoder passwordEncoder;
    private JwtUtil jwtUtil;
    private TokenBlacklist tokenBlacklist;
    private LoginRateLimiter rateLimiter;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        jwtUtil = new JwtUtil("test-secret-key-for-unit-test-min-32chars!!", 7200000);
        tokenBlacklist = new TokenBlacklist();
        rateLimiter = new LoginRateLimiter();
        authService = new AuthService(userRepository, passwordEncoder, jwtUtil, tokenBlacklist, rateLimiter);
    }

    @Test
    void login_withValidCredentials_returnsLoginResponseAndSetsCookie() {
        String password = "Abcd1234";
        String hashed = passwordEncoder.encode(password);
        SysRole adminRole = new SysRole();
        adminRole.setRoleCode("ADMIN");
        adminRole.setRoleName("管理员");
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("admin");
        user.setPassword(hashed);
        user.setRealName("管理员");
        user.setStatus("ACTIVE");
        user.setRoles(Set.of(adminRole));

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        LoginResponse loginResponse = authService.login("admin", password, "127.0.0.1", response);

        assertThat(loginResponse).isNotNull();
        assertThat(loginResponse.getUsername()).isEqualTo("admin");
        assertThat(loginResponse.getRealName()).isEqualTo("管理员");

        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(cookieCaptor.capture());
        Cookie setCookie = cookieCaptor.getValue();
        assertThat(setCookie.getName()).isEqualTo("zhenduanqi_token");
        assertThat(setCookie.getValue()).isNotBlank();
        assertThat(setCookie.isHttpOnly()).isTrue();
        assertThat(setCookie.getSecure()).isFalse();
        assertThat(setCookie.getPath()).isEqualTo("/api");
        verify(userRepository).save(any());
    }

    @Test
    void login_withWrongPassword_throwsExceptionAndIncrementsFailCount() {
        String password = "Abcd1234";
        String hashed = passwordEncoder.encode(password);
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("admin");
        user.setPassword(hashed);
        user.setStatus("ACTIVE");

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        assertThatThrownBy(() -> authService.login("admin", "wrong-password", "127.0.0.1", response))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("用户名或密码错误");

        assertThat(user.getFailCount()).isEqualTo(1);
        verify(userRepository).save(user);
    }

    @Test
    void login_whenAccountLocked_throwsException() {
        String password = "Abcd1234";
        String hashed = passwordEncoder.encode(password);
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("admin");
        user.setPassword(hashed);
        user.setStatus("ACTIVE");
        user.setFailCount(5);
        user.setLockUntil(LocalDateTime.now().plusMinutes(5));

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login("admin", password, "127.0.0.1", response))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("已被锁定");

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_whenIpRateLimited_throwsException() {
        String password = "Abcd1234";
        String hashed = passwordEncoder.encode(password);
        SysUser user = new SysUser();
        user.setUsername("admin");
        user.setPassword(hashed);
        user.setStatus("ACTIVE");

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        for (int i = 0; i < 5; i++) {
            String wrongPwd = "wrong-" + i;
            assertThatThrownBy(() -> authService.login("admin", wrongPwd, "10.0.0.1", response))
                    .isInstanceOf(RuntimeException.class);
        }

        assertThatThrownBy(() -> authService.login("admin", password, "10.0.0.1", response))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("过于频繁");
    }

    @Test
    void validateToken_withValidToken_returnsUsername() {
        String token = jwtUtil.generateToken("admin");
        String username = authService.validateToken(token);
        assertThat(username).isEqualTo("admin");
    }

    @Test
    void validateToken_afterLogout_returnsNull() {
        String token = jwtUtil.generateToken("admin");
        authService.logout(token, response);
        assertThat(authService.validateToken(token)).isNull();
    }

    @Test
    void validateToken_withExpiredToken_returnsNull() {
        JwtUtil shortJwt = new JwtUtil("test-secret-key-for-unit-test-min-32chars!!", -1);
        String token = shortJwt.generateToken("admin");
        assertThat(authService.validateToken(token)).isNull();
    }
}
