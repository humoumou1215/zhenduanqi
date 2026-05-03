package com.zhenduanqi.service;

import com.zhenduanqi.entity.CommandGuardRule;
import com.zhenduanqi.repository.CommandGuardRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CommandGuardServiceDebugTest {

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
    void debugPatternMatching() {
        String command = "ognl -x 1 @java.lang.System@out";
        
        List<CommandGuardRule> rules = ruleRepository.findByRuleTypeAndEnabledTrue("BLACKLIST");
        System.out.println("BLACKLIST rules count: " + rules.size());
        
        for (CommandGuardRule rule : rules) {
            System.out.println("Rule pattern: " + rule.getPattern());
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(rule.getPattern());
            boolean matches = p.matcher(command).find();
            System.out.println("  Matches command '" + command + "': " + matches);
        }
        
        CommandGuardService.GuardResult result = commandGuardService.check(command);
        System.out.println("Result blocked: " + result.isBlocked());
        System.out.println("Result reason: " + result.getReason());
        
        assertThat(result.isBlocked()).isTrue();
    }
}
