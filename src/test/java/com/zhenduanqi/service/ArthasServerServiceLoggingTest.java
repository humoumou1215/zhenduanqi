package com.zhenduanqi.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.zhenduanqi.client.ArthasHttpClient;
import com.zhenduanqi.config.TokenEncryptionUtil;
import com.zhenduanqi.dto.ArthasServerDTO;
import com.zhenduanqi.repository.ArthasServerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArthasServerServiceLoggingTest {

    @Mock
    private ArthasServerRepository repository;

    @Mock
    private TokenEncryptionUtil encryptionUtil;

    @Mock
    private ArthasHttpClient arthasHttpClient;

    private ArthasServerService service;

    @BeforeEach
    void setUp() {
        service = new ArthasServerService(repository, encryptionUtil, arthasHttpClient);
    }

    private ListAppender<ILoggingEvent> createListAppender() {
        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger logger = ctx.getLogger(ArthasServerService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private void removeAppender(ListAppender<ILoggingEvent> appender) {
        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        ctx.getLogger(ArthasServerService.class).detachAppender(appender);
    }

    @Test
    void create_logsInfo() {
        ListAppender<ILoggingEvent> appender = createListAppender();
        try {
            ArthasServerDTO dto = new ArthasServerDTO();
            dto.setId("srv1");
            dto.setName("TestServer");
            dto.setHost("localhost");
            dto.setHttpPort(8563);

            when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

            service.create(dto);

            List<ILoggingEvent> events = appender.list;
            assertThat(events.stream().anyMatch(e ->
                    e.getLevel() == Level.INFO && e.getFormattedMessage().contains("服务器创建"))).isTrue();
        } finally {
            removeAppender(appender);
        }
    }

    @Test
    void delete_logsInfo() {
        ListAppender<ILoggingEvent> appender = createListAppender();
        try {
            when(repository.existsById("srv1")).thenReturn(true);

            service.delete("srv1");

            List<ILoggingEvent> events = appender.list;
            assertThat(events.stream().anyMatch(e ->
                    e.getLevel() == Level.INFO && e.getFormattedMessage().contains("服务器删除"))).isTrue();
        } finally {
            removeAppender(appender);
        }
    }
}
