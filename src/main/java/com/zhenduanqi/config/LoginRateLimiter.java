package com.zhenduanqi.config;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimiter {

    private final ConcurrentHashMap<String, RateEntry> attempts = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS = 300_000;

    public boolean isBlocked(String ip) {
        evictExpired();
        RateEntry entry = attempts.get(ip);
        return entry != null && entry.count >= MAX_ATTEMPTS;
    }

    public void recordFailure(String ip) {
        attempts.compute(ip, (key, existing) -> {
            if (existing == null || System.currentTimeMillis() > existing.windowEnd) {
                return new RateEntry(1, System.currentTimeMillis() + WINDOW_MS);
            }
            existing.count++;
            return existing;
        });
    }

    public void reset(String ip) {
        attempts.remove(ip);
    }

    private void evictExpired() {
        long now = System.currentTimeMillis();
        attempts.values().removeIf(entry -> now > entry.windowEnd);
    }

    private static class RateEntry {
        int count;
        long windowEnd;

        RateEntry(int count, long windowEnd) {
            this.count = count;
            this.windowEnd = windowEnd;
        }
    }
}
