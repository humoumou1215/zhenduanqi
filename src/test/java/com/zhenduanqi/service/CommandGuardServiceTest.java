package com.zhenduanqi.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.zhenduanqi.entity.CommandGuardRule;
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
class CommandGuardServiceTest {

    @Mock
    private CommandGuardRuleRepository ruleRepository;

    private CommandGuardService guard;

    @BeforeEach
    void setUp() {
        guard = new CommandGuardService(ruleRepository);
    }

    private void setupRules(List<String> blacklist, List<String> whitelist) {
        List<CommandGuardRule> blackRules = blacklist.stream().map(p -> {
            CommandGuardRule r = new CommandGuardRule();
            r.setRuleType("BLACKLIST");
            r.setPattern(p);
            r.setEnabled(true);
            return r;
        }).toList();
        List<CommandGuardRule> whiteRules = whitelist.stream().map(p -> {
            CommandGuardRule r = new CommandGuardRule();
            r.setRuleType("WHITELIST");
            r.setPattern(p);
            r.setEnabled(true);
            return r;
        }).toList();

        when(ruleRepository.findByRuleTypeAndEnabledTrue("BLACKLIST")).thenReturn(blackRules);
        when(ruleRepository.findByRuleTypeAndEnabledTrue("WHITELIST")).thenReturn(whiteRules);
        guard.reloadRules();
    }

    @Test
    void check_commandMatchingBlacklist_isBlocked() {
        setupRules(List.of("^ognl\\b"), List.of());

        assertThat(guard.check("ognl -x 1").isBlocked()).isTrue();
    }
    
    @Test
    void check_systemCommand_isExempted() {
        setupRules(List.of("^reset\\b", "^version\\b"), List.of());

        assertThat(guard.check("reset").isBlocked()).isFalse();
        assertThat(guard.check("reset --all").isBlocked()).isFalse();
        assertThat(guard.check("version").isBlocked()).isFalse();
        assertThat(guard.check("VERSION").isBlocked()).isFalse();
    }

    @Test
    void check_safeCommand_isAllowed() {
        setupRules(List.of("^ognl\\b"), List.of());

        assertThat(guard.check("thread -n 5").isBlocked()).isFalse();
        assertThat(guard.check("dashboard").isBlocked()).isFalse();
        assertThat(guard.check("sc -d com.example.MyController").isBlocked()).isFalse();
    }

    @Test
    void check_whitelistOverridesBlacklist() {
        setupRules(List.of("^jad\\b"), List.of("^jad --source-only\\b"));

        assertThat(guard.check("jad --source-only com.example.MyController").isBlocked()).isFalse();
        assertThat(guard.check("jad com.example.MyController").isBlocked()).isTrue();
    }

    @Test
    void check_noRules_allCommandsAllowed() {
        setupRules(List.of(), List.of());

        assertThat(guard.check("ognl -x 1").isBlocked()).isFalse();
        assertThat(guard.check("anything").isBlocked()).isFalse();
    }

    @Test
    void check_returnsReasonOnBlock() {
        setupRules(List.of("^ognl\\b"), List.of());

        CommandGuardService.GuardResult result = guard.check("ognl -x 1");

        assertThat(result.isBlocked()).isTrue();
        assertThat(result.getReason()).contains("ognl");
    }

    private ListAppender<ILoggingEvent> createListAppender() {
        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger logger = ctx.getLogger(CommandGuardService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private void removeAppender(ListAppender<ILoggingEvent> appender) {
        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        ctx.getLogger(CommandGuardService.class).detachAppender(appender);
    }

    @Test
    void reloadRules_logsInfoWithRuleCount() {
        ListAppender<ILoggingEvent> appender = createListAppender();
        try {
            setupRules(List.of("^ognl\\b", "^mc\\b"), List.of("^jad --source-only\\b"));

            List<ILoggingEvent> events = appender.list;
            assertThat(events.stream().anyMatch(e ->
                    e.getLevel() == Level.INFO && e.getFormattedMessage().contains("规则加载"))).isTrue();
            assertThat(events.stream().anyMatch(e ->
                    e.getFormattedMessage().contains("2") && e.getFormattedMessage().contains("1"))).isTrue();
        } finally {
            removeAppender(appender);
        }
    }

    @Test
    void check_commandBlocked_logsWarnWithFullCommandAndDescription() {
        ListAppender<ILoggingEvent> appender = createListAppender();
        try {
            setupRulesWithDescription("^ognl\\b", "OGNL 表达式可执行任意代码");

            guard.check("ognl -x 1");

            List<ILoggingEvent> events = appender.list;
            assertThat(events.stream().anyMatch(e ->
                    e.getLevel() == Level.WARN && e.getFormattedMessage().contains("高危命令拦截"))).isTrue();
            assertThat(events.stream().anyMatch(e ->
                    e.getFormattedMessage().contains("ognl -x 1"))).isTrue();
            assertThat(events.stream().anyMatch(e ->
                    e.getFormattedMessage().contains("OGNL"))).isTrue();
        } finally {
            removeAppender(appender);
        }
    }

    private void setupRulesWithDescription(String pattern, String description) {
        CommandGuardRule rule = new CommandGuardRule();
        rule.setRuleType("BLACKLIST");
        rule.setPattern(pattern);
        rule.setDescription(description);
        rule.setEnabled(true);

        when(ruleRepository.findByRuleTypeAndEnabledTrue("BLACKLIST")).thenReturn(List.of(rule));
        when(ruleRepository.findByRuleTypeAndEnabledTrue("WHITELIST")).thenReturn(List.of());
        guard.reloadRules();
    }
}
