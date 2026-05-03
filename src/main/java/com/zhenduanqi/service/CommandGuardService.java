package com.zhenduanqi.service;

import com.zhenduanqi.entity.CommandGuardRule;
import com.zhenduanqi.repository.CommandGuardRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class CommandGuardService {

    private static final Logger log = LoggerFactory.getLogger(CommandGuardService.class);

    private static final List<String> SYSTEM_COMMANDS = List.of("reset", "version");

    private final CommandGuardRuleRepository ruleRepository;
    private List<Pattern> blacklistPatterns = new ArrayList<>();
    private List<Pattern> whitelistPatterns = new ArrayList<>();

    public CommandGuardService(CommandGuardRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void reloadRules() {
        if (ruleRepository == null) return;
        blacklistPatterns = ruleRepository.findByRuleTypeAndEnabledTrue("BLACKLIST")
                .stream().map(r -> Pattern.compile(r.getPattern())).toList();
        whitelistPatterns = ruleRepository.findByRuleTypeAndEnabledTrue("WHITELIST")
                .stream().map(r -> Pattern.compile(r.getPattern())).toList();
        log.info("规则加载完成: blacklist={}, whitelist={}", blacklistPatterns.size(), whitelistPatterns.size());
    }

    public GuardResult check(String command) {
        String trimmedCommand = command.trim();
        String commandName = trimmedCommand.split("\\s+")[0].toLowerCase();
        
        if (SYSTEM_COMMANDS.contains(commandName)) {
            log.debug("系统命令豁免: {}", commandName);
            return new GuardResult(false, null);
        }
        
        for (Pattern p : whitelistPatterns) {
            if (p.matcher(trimmedCommand).find()) {
                return new GuardResult(false, null);
            }
        }
        for (Pattern p : blacklistPatterns) {
            if (p.matcher(trimmedCommand).find()) {
                log.warn("命令拦截: command={}, pattern={}", commandName, p.pattern());
                return new GuardResult(true, "高危命令已被拦截: " + commandName);
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
