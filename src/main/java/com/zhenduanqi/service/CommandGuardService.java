package com.zhenduanqi.service;

import com.zhenduanqi.entity.CommandGuardRule;
import com.zhenduanqi.repository.CommandGuardRuleRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class CommandGuardService {

    private final CommandGuardRuleRepository ruleRepository;
    private List<Pattern> blacklistPatterns = new ArrayList<>();
    private List<Pattern> whitelistPatterns = new ArrayList<>();

    public CommandGuardService(CommandGuardRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    @PostConstruct
    public void reloadRules() {
        if (ruleRepository == null) return;
        blacklistPatterns = ruleRepository.findByRuleTypeAndEnabledTrue("BLACKLIST")
                .stream().map(r -> Pattern.compile(r.getPattern())).toList();
        whitelistPatterns = ruleRepository.findByRuleTypeAndEnabledTrue("WHITELIST")
                .stream().map(r -> Pattern.compile(r.getPattern())).toList();
    }

    public GuardResult check(String command) {
        for (Pattern p : whitelistPatterns) {
            if (p.matcher(command).find()) {
                return new GuardResult(false, null);
            }
        }
        for (Pattern p : blacklistPatterns) {
            if (p.matcher(command).find()) {
                return new GuardResult(true, "高危命令已被拦截: " + command.split("\\s")[0]);
            }
        }
        return new GuardResult(false, null);
    }

    public List<CommandGuardRule> getAllRules() {
        return ruleRepository.findAll();
    }

    public CommandGuardRule addRule(CommandGuardRule rule) {
        CommandGuardRule saved = ruleRepository.save(rule);
        reloadRules();
        return saved;
    }

    public CommandGuardRule updateRule(Long id, CommandGuardRule rule) {
        CommandGuardRule existing = ruleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("规则不存在"));
        existing.setRuleType(rule.getRuleType());
        existing.setPattern(rule.getPattern());
        existing.setDescription(rule.getDescription());
        existing.setEnabled(rule.isEnabled());
        CommandGuardRule saved = ruleRepository.save(existing);
        reloadRules();
        return saved;
    }

    public void deleteRule(Long id) {
        ruleRepository.deleteById(id);
        reloadRules();
    }

    public static class GuardResult {
        private final boolean blocked;
        private final String reason;

        public GuardResult(boolean blocked, String reason) {
            this.blocked = blocked;
            this.reason = reason;
        }

        public boolean isBlocked() { return blocked; }
        public String getReason() { return reason; }
    }
}
