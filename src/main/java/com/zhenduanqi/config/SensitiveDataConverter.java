package com.zhenduanqi.config;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SensitiveDataConverter extends MessageConverter {

    private static final List<MaskRule> MASK_RULES = List.of(
            new MaskRule(Pattern.compile("(password=)[^,\\s}&]*"), "$1******"),
            new MaskRule(Pattern.compile("(\"password\"\\s*:\\s*\")[^\"]*\""), "$1******\""),
            new MaskRule(Pattern.compile("(token=)[^,\\s}&]*"), "$1******"),
            new MaskRule(Pattern.compile("(\"token\"\\s*:\\s*\")[^\"]*\""), "$1******\""),
            new MaskRule(Pattern.compile("(Authorization:\\s*Bearer\\s+)\\S+"), "$1******")
    );

    @Override
    public String convert(ILoggingEvent event) {
        String message = super.convert(event);
        if (message == null || message.isEmpty()) {
            return message;
        }
        for (MaskRule rule : MASK_RULES) {
            message = rule.apply(message);
        }
        return message;
    }

    private static class MaskRule {
        final Pattern pattern;
        final String replacement;

        MaskRule(Pattern pattern, String replacement) {
            this.pattern = pattern;
            this.replacement = replacement;
        }

        String apply(String message) {
            return pattern.matcher(message).replaceAll(replacement);
        }
    }
}
