package com.zhenduanqi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TokenEncryptionConfig {

    @Value("${arthas.server.token-secret}")
    private String tokenSecret;

    @Bean
    public TokenEncryptionUtil tokenEncryptionUtil() {
        return new TokenEncryptionUtil(tokenSecret);
    }
}
