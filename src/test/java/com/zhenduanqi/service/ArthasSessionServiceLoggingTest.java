package com.zhenduanqi.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.zhenduanqi.client.ArthasHttpClient;
import com.zhenduanqi.dto.ArthasSessionDTO;
import com.zhenduanqi.dto.CreateSessionRequest;
import com.zhenduanqi.entity.ArthasSession;
import com.zhenduanqi.model.ArthasApiResponse;
import com.zhenduanqi.model.SessionInfo;
import com.zhenduanqi.model.ServerInfo;
import com.zhenduanqi.repository.ArthasServerRepository;
import com.zhenduanqi.repository.ArthasSessionRepository;
import com.zhenduanqi.repository.CommandGuardRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArthasSessionServiceLoggingTest {

    @Mock
    private ArthasSessionRepository sessionRepository;
    @Mock
    private ArthasServerService serverService;
    @Mock
    private ArthasHttpClient arthasClient;
    @Mock
    private CommandGuardRuleRepository ruleRepository;
    @Mock
    private CommandGuardService commandGuardService;

    private ArthasSessionService sessionService;

    @BeforeEach
    void setUp() {
        sessionService = new ArthasSessionService(
                sessionRepository, serverService, arthasClient, commandGuardService);
    }

    private ListAppender<ILoggingEvent> createListAppender() {
        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger logger = ctx.getLogger(ArthasSessionService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private void removeAppender(ListAppender<ILoggingEvent> appender) {
        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        ctx.getLogger(ArthasSessionService.class).detachAppender(appender);
    }

    @Test
    void createSession_success_logsInfo() {
        ListAppender<ILoggingEvent> appender = createListAppender();
        try {
            ServerInfo serverInfo = new ServerInfo();
            serverInfo.setId("server1");
            serverInfo.setName("TestServer");

            when(serverService.findServerInfoById("server1")).thenReturn(Optional.of(serverInfo));
            when(commandGuardService.check(anyString())).thenReturn(new CommandGuardService.GuardResult(false, null));
            when(arthasClient.initSession(serverInfo)).thenReturn(new SessionInfo("arthas-session-1", "consumer-1"));

            ArthasApiResponse apiResponse = new ArthasApiResponse();
            apiResponse.setState("scheduled");
            ArthasApiResponse.Body body = new ArthasApiResponse.Body();
            body.setJobId(1);
            apiResponse.setBody(body);
            when(arthasClient.asyncExecuteCommand(any(), anyString(), anyString())).thenReturn(apiResponse);
            when(sessionRepository.save(any(ArthasSession.class))).thenAnswer(inv -> {
                ArthasSession s = inv.getArgument(0);
                s.setId(1L);
                return s;
            });

            CreateSessionRequest request = new CreateSessionRequest();
            request.setServerId("server1");
            request.setCommand("thread -n 5");
            request.setSceneId(1L);
            request.setStepId(1L);

            sessionService.createSession(request, "admin");

            List<ILoggingEvent> events = appender.list;
            assertThat(events.stream().anyMatch(e ->
                    e.getLevel() == Level.INFO && e.getFormattedMessage().contains("创建 Arthas 会话"))).isTrue();
        } finally {
            removeAppender(appender);
        }
    }

    @Test
    void closeSession_logsInfo() {
        ListAppender<ILoggingEvent> appender = createListAppender();
        try {
            ServerInfo serverInfo = new ServerInfo();
            serverInfo.setId("server1");

            ArthasSession session = new ArthasSession();
            session.setId(1L);
            session.setServerId("server1");
            session.setArthasSessionId("arthas-session-1");
            session.setStatus("ACTIVE");

            when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
            when(serverService.findServerInfoById("server1")).thenReturn(Optional.of(serverInfo));
            when(arthasClient.executeSystemCommand(any(), anyString(), anyString())).thenReturn(null);
            when(arthasClient.closeSession(any(), anyString())).thenReturn(true);
            when(sessionRepository.save(any(ArthasSession.class))).thenReturn(session);

            sessionService.closeSession(1L);

            List<ILoggingEvent> events = appender.list;
            assertThat(events.stream().anyMatch(e ->
                    e.getLevel() == Level.INFO && e.getFormattedMessage().contains("关闭 Arthas 会话"))).isTrue();
        } finally {
            removeAppender(appender);
        }
    }

    @Test
    void interruptJob_logsInfo() {
        ListAppender<ILoggingEvent> appender = createListAppender();
        try {
            ServerInfo serverInfo = new ServerInfo();
            serverInfo.setId("server1");

            ArthasSession session = new ArthasSession();
            session.setId(1L);
            session.setServerId("server1");
            session.setArthasSessionId("arthas-session-1");

            when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
            when(serverService.findServerInfoById("server1")).thenReturn(Optional.of(serverInfo));
            when(arthasClient.interruptJob(any(), anyString())).thenReturn(true);

            sessionService.interruptJob(1L);

            List<ILoggingEvent> events = appender.list;
            assertThat(events.stream().anyMatch(e ->
                    e.getLevel() == Level.INFO && e.getFormattedMessage().contains("中断任务"))).isTrue();
        } finally {
            removeAppender(appender);
        }
    }

    @Test
    void cleanupOrphanSessions_logsInfo() {
        ListAppender<ILoggingEvent> appender = createListAppender();
        try {
            ArthasSession orphanSession = new ArthasSession();
            orphanSession.setId(1L);
            orphanSession.setServerId("server1");
            orphanSession.setArthasSessionId("arthas-session-1");
            orphanSession.setStatus("ACTIVE");
            orphanSession.setLastActiveAt(LocalDateTime.now().minusMinutes(15));

            when(sessionRepository.findByLastActiveAtBeforeAndStatus(any(LocalDateTime.class), eq("ACTIVE")))
                    .thenReturn(List.of(orphanSession));
            when(sessionRepository.findById(1L)).thenReturn(Optional.of(orphanSession));
            when(serverService.findServerInfoById("server1")).thenReturn(Optional.empty());
            when(sessionRepository.save(any(ArthasSession.class))).thenReturn(orphanSession);

            sessionService.cleanupOrphanSessions();

            List<ILoggingEvent> events = appender.list;
            assertThat(events.stream().anyMatch(e ->
                    e.getLevel() == Level.INFO && e.getFormattedMessage().contains("清理孤儿会话完成"))).isTrue();
        } finally {
            removeAppender(appender);
        }
    }

    @Test
    void cleanupOrphanSessions_whenEmpty_logsInfo() {
        ListAppender<ILoggingEvent> appender = createListAppender();
        try {
            when(sessionRepository.findByLastActiveAtBeforeAndStatus(any(LocalDateTime.class), eq("ACTIVE")))
                    .thenReturn(List.of());

            sessionService.cleanupOrphanSessions();

            List<ILoggingEvent> events = appender.list;
            assertThat(events.stream().anyMatch(e ->
                    e.getLevel() == Level.INFO && e.getFormattedMessage().contains("清理孤儿会话完成"))).isTrue();
        } finally {
            removeAppender(appender);
        }
    }
}
