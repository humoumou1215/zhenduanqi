package com.zhenduanqi.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilLoggingTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil("test-secret-key-for-unit-test-min-32chars!!", 7200000);
    }

    private ListAppender<ILoggingEvent> createListAppender() {
        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger logger = ctx.getLogger(JwtUtil.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private void removeAppender(ListAppender<ILoggingEvent> appender) {
        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        ctx.getLogger(JwtUtil.class).detachAppender(appender);
    }

    @Test
    void validateAndGetUsername_invalidToken_logsWarn() {
        ListAppender<ILoggingEvent> appender = createListAppender();
        try {
            assertThatThrownBy(() -> jwtUtil.validateAndGetUsername("invalid-token"))
                    .isInstanceOf(Exception.class);

            List<ILoggingEvent> events = appender.list;
            assertThat(events.stream().anyMatch(e ->
                    e.getLevel() == Level.WARN && e.getFormattedMessage().contains("Token校验异常"))).isTrue();
        } finally {
            removeAppender(appender);
        }
    }

    @Test
    void validateAndGetUsername_invalidToken_doesNotLogTokenValue() {
        ListAppender<ILoggingEvent> appender = createListAppender();
        try {
            assertThatThrownBy(() -> jwtUtil.validateAndGetUsername("secret-token-value-12345"))
                    .isInstanceOf(Exception.class);

            List<ILoggingEvent> events = appender.list;
            assertThat(events.stream().noneMatch(e ->
                    e.getFormattedMessage().contains("secret-token-value-12345"))).isTrue();
        } finally {
            removeAppender(appender);
        }
    }
}
