package com.zhenduanqi.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.zhenduanqi.client.ArthasHttpClient;
import com.zhenduanqi.entity.CommandGuardRule;
import com.zhenduanqi.repository.ArthasServerRepository;
import com.zhenduanqi.repository.CommandGuardRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArthasExecuteServiceLoggingTest {

    @Mock
    private ArthasServerRepository serverRepository;
    @Mock
    private CommandGuardRuleRepository ruleRepository;

    private ArthasExecuteService executeService;
    private CommandGuardService guardService;

    @BeforeEach
    void setUp() {
        guardService = new CommandGuardService(ruleRepository);
        ArthasHttpClient arthasClient = new ArthasHttpClient();
        executeService = new ArthasExecuteService(serverRepository, arthasClient, guardService);
    }

    private ListAppender<ILoggingEvent> createListAppender() {
        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger logger = ctx.getLogger(ArthasExecuteService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private void removeAppender(ListAppender<ILoggingEvent> appender) {
        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        ctx.getLogger(ArthasExecuteService.class).detachAppender(appender);
    }

    @Test
    void execute_blockedCommand_logsStart() {
        ListAppender<ILoggingEvent> appender = createListAppender();
        try {
            CommandGuardRule rule = new CommandGuardRule();
            rule.setRuleType("BLACKLIST");
            rule.setPattern("^ognl\\b");
            rule.setEnabled(true);
            when(ruleRepository.findByRuleTypeAndEnabledTrue("BLACKLIST")).thenReturn(List.of(rule));
            when(ruleRepository.findByRuleTypeAndEnabledTrue("WHITELIST")).thenReturn(List.of());
            guardService.reloadRules();

            executeService.execute("server1", "ognl -x 1");

            List<ILoggingEvent> events = appender.list;
            assertThat(events.stream().anyMatch(e ->
                    e.getLevel() == Level.INFO && e.getFormattedMessage().contains("命令执行开始"))).isTrue();
        } finally {
            removeAppender(appender);
        }
    }
}
