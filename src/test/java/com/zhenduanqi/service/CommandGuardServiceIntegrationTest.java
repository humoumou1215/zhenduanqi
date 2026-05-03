package com.zhenduanqi.service;

import com.zhenduanqi.entity.CommandGuardRule;
import com.zhenduanqi.repository.CommandGuardRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CommandGuardServiceIntegrationTest {

    @Autowired
    private CommandGuardService commandGuardService;

    @Autowired
    private CommandGuardRuleRepository ruleRepository;

    @BeforeEach
    void setUp() {
        ruleRepository.deleteAll();
        
        CommandGuardRule ognlRule = new CommandGuardRule();
        ognlRule.setRuleType("BLACKLIST");
        ognlRule.setPattern("^ognl\\b");
        ognlRule.setDescription("OGNL 表达式可执行任意代码，极高风险");
        ognlRule.setEnabled(true);
        ruleRepository.save(ognlRule);
        
        commandGuardService.reloadRules();
    }

    @Test
    void testOgnlCommandIsBlocked() {
        CommandGuardService.GuardResult result = commandGuardService.check("ognl -x 1 @java.lang.System@out");
        assertThat(result.isBlocked()).isTrue();
    }

    @Test
    void testThreadCommandIsAllowed() {
        CommandGuardService.GuardResult result = commandGuardService.check("thread -n 5");
        assertThat(result.isBlocked()).isFalse();
    }

    @Test
    void testRulesAreLoaded() {
        var rules = ruleRepository.findAll();
        assertThat(rules).isNotEmpty();
    }
}
