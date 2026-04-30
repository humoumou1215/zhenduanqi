package com.zhenduanqi.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CommandGuardServiceTest {

    private CommandGuardService guard;

    @BeforeEach
    void setUp() {
        guard = new CommandGuardService(null);
    }

    @Test
    void check_commandMatchingBlacklist_isBlocked() {
        guard.setBlacklistPatterns(List.of("^ognl\\b", "^reset\\b"));

        assertThat(guard.check("ognl -x 1").isBlocked()).isTrue();
        assertThat(guard.check("reset").isBlocked()).isTrue();
        assertThat(guard.check("reset --all").isBlocked()).isTrue();
    }

    @Test
    void check_safeCommand_isAllowed() {
        guard.setBlacklistPatterns(List.of("^ognl\\b", "^reset\\b"));

        assertThat(guard.check("thread -n 5").isBlocked()).isFalse();
        assertThat(guard.check("dashboard").isBlocked()).isFalse();
        assertThat(guard.check("sc -d com.example.MyController").isBlocked()).isFalse();
    }

    @Test
    void check_whitelistOverridesBlacklist() {
        guard.setBlacklistPatterns(List.of("^jad\\b"));
        guard.setWhitelistPatterns(List.of("^jad --source-only\\b"));

        assertThat(guard.check("jad --source-only com.example.MyController").isBlocked()).isFalse();
        assertThat(guard.check("jad com.example.MyController").isBlocked()).isTrue();
    }

    @Test
    void check_noRules_allCommandsAllowed() {
        guard.setBlacklistPatterns(List.of());
        guard.setWhitelistPatterns(List.of());

        assertThat(guard.check("ognl -x 1").isBlocked()).isFalse();
        assertThat(guard.check("anything").isBlocked()).isFalse();
    }

    @Test
    void check_returnsReasonOnBlock() {
        guard.setBlacklistPatterns(List.of("^ognl\\b"));

        CommandGuardService.GuardResult result = guard.check("ognl -x 1");

        assertThat(result.isBlocked()).isTrue();
        assertThat(result.getReason()).contains("ognl");
    }
}
