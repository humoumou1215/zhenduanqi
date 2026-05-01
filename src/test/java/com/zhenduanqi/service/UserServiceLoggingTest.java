package com.zhenduanqi.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.zhenduanqi.dto.CreateUserRequest;
import com.zhenduanqi.entity.SysUser;
import com.zhenduanqi.repository.SysRoleRepository;
import com.zhenduanqi.repository.SysUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceLoggingTest {

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

    private ListAppender<ILoggingEvent> createListAppender() {
        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger logger = ctx.getLogger(UserService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private void removeAppender(ListAppender<ILoggingEvent> appender) {
        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        ctx.getLogger(UserService.class).detachAppender(appender);
    }

    @Test
    void create_logsInfo() {
        ListAppender<ILoggingEvent> appender = createListAppender();
        try {
            CreateUserRequest req = new CreateUserRequest();
            req.setUsername("newuser");
            req.setPassword("Abcd1234");
            req.setRealName("New User");

            when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
            when(userRepository.save(any())).thenAnswer(i -> {
                SysUser u = i.getArgument(0);
                u.setId(1L);
                return u;
            });

            userService.create(req);

            List<ILoggingEvent> events = appender.list;
            assertThat(events.stream().anyMatch(e ->
                    e.getLevel() == Level.INFO && e.getFormattedMessage().contains("用户创建"))).isTrue();
            assertThat(events.stream().anyMatch(e ->
                    e.getFormattedMessage().contains("newuser"))).isTrue();
        } finally {
            removeAppender(appender);
        }
    }

    @Test
    void resetPassword_logsInfo() {
        ListAppender<ILoggingEvent> appender = createListAppender();
        try {
            SysUser user = new SysUser();
            user.setId(1L);
            user.setUsername("testuser");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            userService.resetPassword(1L, "NewPass123");

            List<ILoggingEvent> events = appender.list;
            assertThat(events.stream().anyMatch(e ->
                    e.getLevel() == Level.INFO && e.getFormattedMessage().contains("密码重置"))).isTrue();
        } finally {
            removeAppender(appender);
        }
    }
}
