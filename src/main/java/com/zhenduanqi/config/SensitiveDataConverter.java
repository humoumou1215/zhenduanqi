package com.zhenduanqi.config;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.List;
import java.util.regex.Pattern;

public class SensitiveDataConverter extends MessageConverter {

    private static final List<Pattern> SENSITIVE_PATTERNS = List.of(
            Pattern.compile("(password=)[^,\\s}]*"),
            Pattern.compile("(password\":\")[^\"]*\""),
            Pattern.compile("(token=)[^,\\s}]*"),
            Pattern.compile("(token\":\")[^\"]*\""),
            Pattern.compile("(Authorization:\\s*Bearer\\s+)\\S+")
    );

    private static final List<String> REPLACEMENTS = List.of(
            "${1}******",
            "${1}******\"",
            "${1}******",
            "${1}******\"",
            "${1}******"
    );

    @Override
    public String convert(ILoggingEvent event) {
        String message = super.convert(event);
        if (message == null || message.isEmpty()) {
            return message;
        }
        for (int i = 0; i < SENSITIVE_PATTERNS.size(); i++) {
            message = SENSITIVE_PATTERNS.get(i).matcher(message).replaceAll(REPLACEMENTS.get(i));
        }
        return message;
    }
}
