package com.zhenduanqi.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.zhenduanqi.entity.SysRole;
import com.zhenduanqi.entity.SysUser;
import com.zhenduanqi.repository.SysUserRepository;
import com.zhenduanqi.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthInterceptorLogTest {

    @Mock
    private AuthService authService;
    @Mock
    private SysUserRepository userRepository;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private StringWriter stringWriter;
    @Mock
    private PrintWriter printWriter;

    private AuthInterceptor authInterceptor;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        authInterceptor = new AuthInterceptor(authService, userRepository);

        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger logger = ctx.getLogger(AuthInterceptor.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        ctx.getLogger(AuthInterceptor.class).detachAppender(appender);
    }

    @Test
    void tokenMissing_logsWarnWithMissingKeyword() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/servers");
        when(request.getCookies()).thenReturn(null);
        when(response.getWriter()).thenReturn(printWriter);

        authInterceptor.preHandle(request, response, null);

        assertThat(appender.list.stream().anyMatch(e ->
                e.getLevel() == Level.WARN && e.getFormattedMessage().contains("Token缺失"))).isTrue();
    }

    @Test
    void tokenEmpty_logsWarnWithMissingKeyword() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/servers");
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("zhenduanqi_token", "")});
        when(authService.validateToken("")).thenReturn(null);
        when(response.getWriter()).thenReturn(printWriter);

        authInterceptor.preHandle(request, response, null);

        assertThat(appender.list.stream().anyMatch(e ->
                e.getLevel() == Level.WARN && e.getFormattedMessage().contains("Token缺失"))).isTrue();
    }

    @Test
    void tokenBlacklisted_logsWarnWithRevokedKeyword() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/servers");
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("zhenduanqi_token", "some-token")});
        when(authService.validateToken("some-token")).thenReturn(null);
        when(authService.isTokenBlacklisted("some-token")).thenReturn(true);
        when(response.getWriter()).thenReturn(printWriter);

        authInterceptor.preHandle(request, response, null);

        assertThat(appender.list.stream().anyMatch(e ->
                e.getLevel() == Level.WARN && e.getFormattedMessage().contains("Token已吊销"))).isTrue();
    }

    @Test
    void tokenInvalid_logsWarnWithInvalidKeyword() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/servers");
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("zhenduanqi_token", "invalid-token")});
        when(authService.validateToken("invalid-token")).thenReturn(null);
        when(authService.isTokenBlacklisted("invalid-token")).thenReturn(false);
        when(response.getWriter()).thenReturn(printWriter);

        authInterceptor.preHandle(request, response, null);

        assertThat(appender.list.stream().anyMatch(e ->
                e.getLevel() == Level.WARN && e.getFormattedMessage().contains("Token无效"))).isTrue();
    }

    @Test
    void validToken_doesNotLogError() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/servers");
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("zhenduanqi_token", "valid-token")});
        when(authService.validateToken("valid-token")).thenReturn("admin");

        SysRole role = new SysRole();
        role.setRoleCode("ADMIN");
        SysUser user = new SysUser();
        user.setRoles(Set.of(role));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        boolean result = authInterceptor.preHandle(request, response, null);

        assertThat(result).isTrue();
        assertThat(appender.list.stream().noneMatch(e ->
                e.getLevel() == Level.ERROR)).isTrue();
    }

    @Test
    void userNotFound_logsWarnWithUserNotFoundKeyword() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/servers");
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("zhenduanqi_token", "valid-token")});
        when(authService.validateToken("valid-token")).thenReturn("unknown-user");
        when(userRepository.findByUsername("unknown-user")).thenReturn(Optional.empty());
        when(response.getWriter()).thenReturn(printWriter);

        authInterceptor.preHandle(request, response, null);

        assertThat(appender.list.stream().anyMatch(e ->
                e.getLevel() == Level.WARN && e.getFormattedMessage().contains("用户不存在"))).isTrue();
    }
}
