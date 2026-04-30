package com.zhenduanqi.config;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenBlacklist {

    private final ConcurrentHashMap<String, Long> blacklist = new ConcurrentHashMap<>();

    public void add(String token, long expiryMs) {
        blacklist.put(token, System.currentTimeMillis() + expiryMs);
    }

    public boolean contains(String token) {
        evictExpired();
        return blacklist.containsKey(token);
    }

    private void evictExpired() {
        long now = System.currentTimeMillis();
        blacklist.values().removeIf(expiry -> expiry <= now);
    }
}
