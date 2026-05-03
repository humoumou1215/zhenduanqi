package com.zhenduanqi.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.zhenduanqi.config.JwtUtil;
import com.zhenduanqi.config.SensitiveDataConverter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuthServiceLogTest {

    private JwtUtil jwtUtil;
    private ListAppender<ILoggingEvent> appender;
    private SensitiveDataConverter converter;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil("test-secret-key-for-unit-test-min-32chars!!", 7200000);
        converter = new SensitiveDataConverter();

        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger logger = ctx.getLogger(JwtUtil.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        ctx.getLogger(JwtUtil.class).detachAppender(appender);
    }

    @Test
    void expiredToken_logsWarnWithExpiredKeyword() {
        JwtUtil shortJwt = new JwtUtil("test-secret-key-for-unit-test-min-32chars!!", -1000);
        String token = shortJwt.generateToken("admin");

        try {
            jwtUtil.validateAndGetUsername(token);
        } catch (Exception ignored) {
        }

        assertThat(appender.list.stream().anyMatch(e ->
                e.getLevel() == Level.WARN && e.getFormattedMessage().contains("Token已过期"))).isTrue();
    }

    @Test
    void invalidToken_logsWarnWithInvalidKeyword() {
        try {
            jwtUtil.validateAndGetUsername("invalid-token-format");
        } catch (Exception ignored) {
        }

        assertThat(appender.list.stream().anyMatch(e ->
                e.getLevel() == Level.WARN && e.getFormattedMessage().contains("Token无效"))).isTrue();
    }

    @Test
    void sensitiveDataConverter_masksPasswordField() {
        String originalMessage = "登录失败: 密码错误, username=test, password=MySecretPassword123!, failCount=1";

        ch.qos.logback.classic.spi.ILoggingEvent mockEvent = mock(ILoggingEvent.class);
        when(mockEvent.getFormattedMessage()).thenReturn(originalMessage);

        String result = converter.convert(mockEvent);
        assertThat(result).doesNotContain("MySecretPassword123!");
        assertThat(result).contains("password=******");
    }
}
