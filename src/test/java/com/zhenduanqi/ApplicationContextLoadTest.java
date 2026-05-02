package com.zhenduanqi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
class ApplicationContextLoadTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
        assertNotNull(applicationContext, "Application context should load successfully");
    }

    @Test
    void entityManagerFactoryBeanExists() {
        Object emf = applicationContext.getBean("entityManagerFactory");
        assertNotNull(emf, "entityManagerFactory bean should be created, which requires ByteBuddy at runtime");
    }
}
