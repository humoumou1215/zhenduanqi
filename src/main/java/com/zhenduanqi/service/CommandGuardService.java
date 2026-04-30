package com.zhenduanqi.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class CommandGuardService {

    private List<Pattern> blacklistPatterns = new ArrayList<>();
    private List<Pattern> whitelistPatterns = new ArrayList<>();

    public CommandGuardService(Object unused) {
    }

    public void setBlacklistPatterns(List<String> patterns) {
        this.blacklistPatterns = patterns.stream().map(Pattern::compile).toList();
    }

    public void setWhitelistPatterns(List<String> patterns) {
        this.whitelistPatterns = patterns.stream().map(Pattern::compile).toList();
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
