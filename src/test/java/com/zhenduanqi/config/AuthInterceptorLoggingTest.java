package com.zhenduanqi.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.zhenduanqi.repository.SysUserRepository;
import com.zhenduanqi.service.AuthService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AuthInterceptorLoggingTest {

    @Mock
    private SysUserRepository userRepository;

    private AuthInterceptor interceptor;
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil("test-secret-key-for-unit-test-min-32chars!!", 7200000);
        TokenBlacklist tokenBlacklist = new TokenBlacklist();
        LoginRateLimiter rateLimiter = new LoginRateLimiter();
        AuthService authService = new AuthService(userRepository, new BCryptPasswordEncoder(), jwtUtil, tokenBlacklist, rateLimiter);
        interceptor = new AuthInterceptor(authService);
    }

    private ListAppender<ILoggingEvent> createListAppender() {
        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger logger = ctx.getLogger(AuthInterceptor.class);
        logger.setLevel(Level.DEBUG);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private void removeAppender(ListAppender<ILoggingEvent> appender) {
        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger logger = ctx.getLogger(AuthInterceptor.class);
        logger.detachAppender(appender);
        logger.setLevel(null);
    }

    @Test
    void preHandle_authPassed_logsDebug() throws Exception {
        ListAppender<ILoggingEvent> appender = createListAppender();
        try {
            String validToken = jwtUtil.generateToken("admin");
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/servers");
            request.setCookies(new Cookie("zhenduanqi_token", validToken));
            MockHttpServletResponse response = new MockHttpServletResponse();

            interceptor.preHandle(request, response, null);

            List<ILoggingEvent> events = appender.list;
            assertThat(events.stream().anyMatch(e ->
                    e.getLevel() == Level.DEBUG && e.getFormattedMessage().contains("认证通过"))).isTrue();
            assertThat(events.stream().anyMatch(e ->
                    e.getFormattedMessage().contains("admin"))).isTrue();
        } finally {
            removeAppender(appender);
        }
    }

    @Test
    void preHandle_tokenMissing_logsWarn() throws Exception {
        ListAppender<ILoggingEvent> appender = createListAppender();
        try {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/servers");
            MockHttpServletResponse response = new MockHttpServletResponse();

            interceptor.preHandle(request, response, null);

            List<ILoggingEvent> events = appender.list;
            assertThat(events.stream().anyMatch(e ->
                    e.getLevel() == Level.WARN && e.getFormattedMessage().contains("Token无效或缺失"))).isTrue();
        } finally {
            removeAppender(appender);
        }
    }

    @Test
    void preHandle_tokenInvalid_logsWarn() throws Exception {
        ListAppender<ILoggingEvent> appender = createListAppender();
        try {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/servers");
            request.setCookies(new Cookie("zhenduanqi_token", "bad-token"));
            MockHttpServletResponse response = new MockHttpServletResponse();

            interceptor.preHandle(request, response, null);

            List<ILoggingEvent> events = appender.list;
            assertThat(events.stream().anyMatch(e ->
                    e.getLevel() == Level.WARN && e.getFormattedMessage().contains("Token无效或缺失"))).isTrue();
        } finally {
            removeAppender(appender);
        }
    }
}
