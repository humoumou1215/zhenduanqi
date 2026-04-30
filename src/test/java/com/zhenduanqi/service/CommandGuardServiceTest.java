package com.zhenduanqi.service;

import com.zhenduanqi.entity.CommandGuardRule;
import com.zhenduanqi.repository.CommandGuardRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
        setupRules(List.of("^ognl\\b", "^reset\\b"), List.of());

        assertThat(guard.check("ognl -x 1").isBlocked()).isTrue();
        assertThat(guard.check("reset").isBlocked()).isTrue();
        assertThat(guard.check("reset --all").isBlocked()).isTrue();
    }

    @Test
    void check_safeCommand_isAllowed() {
        setupRules(List.of("^ognl\\b", "^reset\\b"), List.of());

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
}
