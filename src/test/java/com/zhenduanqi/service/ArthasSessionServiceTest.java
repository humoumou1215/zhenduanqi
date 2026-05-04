package com.zhenduanqi.service;

import com.zhenduanqi.client.ArthasHttpClient;
import com.zhenduanqi.dto.ArthasSessionDTO;
import com.zhenduanqi.dto.CreateSessionRequest;
import com.zhenduanqi.entity.ArthasSession;
import com.zhenduanqi.model.ArthasApiResponse;
import com.zhenduanqi.model.ArthasResult;
import com.zhenduanqi.model.ServerInfo;
import com.zhenduanqi.model.SessionInfo;
import com.zhenduanqi.repository.ArthasSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArthasSessionServiceTest {

    @Mock
    private ArthasSessionRepository sessionRepository;

    @Mock
    private ArthasServerService serverService;

    @Mock
    private ArthasHttpClient arthasClient;

    @Mock
    private CommandGuardService commandGuardService;

    private ArthasSessionService sessionService;

    @BeforeEach
    void setUp() {
        sessionService = new ArthasSessionService(
                sessionRepository, serverService, arthasClient, commandGuardService);
    }

    private ServerInfo createTestServerInfo() {
        ServerInfo server = new ServerInfo();
        server.setId("server1");
        server.setName("Test Server");
        server.setHost("localhost");
        server.setHttpPort(8563);
        return server;
    }

    @Test
    void createSession_success() {
        ServerInfo serverInfo = createTestServerInfo();
        when(serverService.findServerInfoById("server1")).thenReturn(Optional.of(serverInfo));
        when(commandGuardService.check("thread -n 5")).thenReturn(
                new CommandGuardService.GuardResult(false, null));
        when(arthasClient.initSession(serverInfo)).thenReturn(
                new SessionInfo("arthas-session-1", "consumer-1"));

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

        ArthasSessionDTO result = sessionService.createSession(request, "admin");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getServerId()).isEqualTo("server1");
        assertThat(result.getArthasSessionId()).isEqualTo("arthas-session-1");
        assertThat(result.getCurrentJobId()).isEqualTo(1);
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getUsername()).isEqualTo("admin");
    }

    @Test
    void createSession_serverNotFound_throwsException() {
        when(serverService.findServerInfoById("nonexistent")).thenReturn(Optional.empty());

        CreateSessionRequest request = new CreateSessionRequest();
        request.setServerId("nonexistent");
        request.setCommand("thread -n 5");

        assertThatThrownBy(() -> sessionService.createSession(request, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("未找到服务器");
    }

    @Test
    void createSession_commandBlocked_throwsException() {
        ServerInfo serverInfo = createTestServerInfo();
        when(serverService.findServerInfoById("server1")).thenReturn(Optional.of(serverInfo));
        when(commandGuardService.check("ognl -x 1")).thenReturn(
                new CommandGuardService.GuardResult(true, "高危命令已被拦截"));

        CreateSessionRequest request = new CreateSessionRequest();
        request.setServerId("server1");
        request.setCommand("ognl -x 1");

        assertThatThrownBy(() -> sessionService.createSession(request, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("命令被拦截");
    }

    @Test
    void createSession_initSessionFails_throwsException() {
        ServerInfo serverInfo = createTestServerInfo();
        when(serverService.findServerInfoById("server1")).thenReturn(Optional.of(serverInfo));
        when(commandGuardService.check("thread -n 5")).thenReturn(
                new CommandGuardService.GuardResult(false, null));
        when(arthasClient.initSession(serverInfo)).thenReturn(null);

        CreateSessionRequest request = new CreateSessionRequest();
        request.setServerId("server1");
        request.setCommand("thread -n 5");

        assertThatThrownBy(() -> sessionService.createSession(request, "admin"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("初始化 Arthas 会话失败");
    }

    @Test
    void getActiveSessions_withServerId_returnsActiveSessions() {
        ArthasSession session1 = new ArthasSession();
        session1.setId(1L);
        session1.setServerId("server1");
        session1.setStatus("ACTIVE");

        ArthasSession session2 = new ArthasSession();
        session2.setId(2L);
        session2.setServerId("server1");
        session2.setStatus("CLOSED");

        when(sessionRepository.findByServerIdAndStatusOrderByCreatedAtDesc("server1", "ACTIVE"))
                .thenReturn(List.of(session1));

        List<ArthasSessionDTO> result = sessionService.getActiveSessions("server1", null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    @Test
    void getActiveSessions_withoutServerId_returnsAllActiveSessions() {
        ArthasSession session1 = new ArthasSession();
        session1.setId(1L);
        session1.setServerId("server1");
        session1.setStatus("ACTIVE");

        ArthasSession session2 = new ArthasSession();
        session2.setId(2L);
        session2.setServerId("server2");
        session2.setStatus("ACTIVE");

        when(sessionRepository.findByStatusOrderByCreatedAtDesc("ACTIVE"))
                .thenReturn(List.of(session1, session2));

        List<ArthasSessionDTO> result = sessionService.getActiveSessions(null, null);

        assertThat(result).hasSize(2);
    }

    @Test
    void getActiveSessions_withUsername_returnsActiveSessions() {
        ArthasSession session1 = new ArthasSession();
        session1.setId(1L);
        session1.setUsername("user1");
        session1.setStatus("ACTIVE");

        when(sessionRepository.findByUsernameAndStatusOrderByCreatedAtDesc("user1", "ACTIVE"))
                .thenReturn(List.of(session1));

        List<ArthasSessionDTO> result = sessionService.getActiveSessions(null, "user1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    @Test
    void getActiveSessions_withServerIdAndUsername_returnsActiveSessions() {
        ArthasSession session1 = new ArthasSession();
        session1.setId(1L);
        session1.setServerId("server1");
        session1.setUsername("user1");
        session1.setStatus("ACTIVE");

        when(sessionRepository.findByServerIdAndUsernameAndStatusOrderByCreatedAtDesc("server1", "user1", "ACTIVE"))
                .thenReturn(List.of(session1));

        List<ArthasSessionDTO> result = sessionService.getActiveSessions("server1", "user1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    @Test
    void pullResults_success() {
        ArthasSession session = new ArthasSession();
        session.setId(1L);
        session.setServerId("server1");
        session.setArthasSessionId("arthas-session-1");
        session.setArthasConsumerId("consumer-1");

        ServerInfo serverInfo = createTestServerInfo();

        ArthasResult result1 = new ArthasResult();
        result1.setType("thread");

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(serverService.findServerInfoById("server1")).thenReturn(Optional.of(serverInfo));
        when(arthasClient.pullResults(serverInfo, "arthas-session-1", "consumer-1"))
                .thenReturn(List.of(result1));
        when(sessionRepository.save(any(ArthasSession.class))).thenReturn(session);

        List<ArthasResult> result = sessionService.pullResults(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isEqualTo("thread");
        verify(sessionRepository).save(any(ArthasSession.class));
    }

    @Test
    void pullResults_sessionNotFound_returnsEmpty() {
        when(sessionRepository.findById(99L)).thenReturn(Optional.empty());

        List<ArthasResult> result = sessionService.pullResults(99L);

        assertThat(result).isEmpty();
    }

    @Test
    void interruptJob_success() {
        ArthasSession session = new ArthasSession();
        session.setId(1L);
        session.setServerId("server1");
        session.setArthasSessionId("arthas-session-1");

        ServerInfo serverInfo = createTestServerInfo();

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(serverService.findServerInfoById("server1")).thenReturn(Optional.of(serverInfo));
        when(arthasClient.interruptJob(serverInfo, "arthas-session-1")).thenReturn(true);

        boolean result = sessionService.interruptJob(1L);

        assertThat(result).isTrue();
        verify(arthasClient).interruptJob(serverInfo, "arthas-session-1");
    }

    @Test
    void interruptJob_sessionNotFound_returnsFalse() {
        when(sessionRepository.findById(99L)).thenReturn(Optional.empty());

        boolean result = sessionService.interruptJob(99L);

        assertThat(result).isFalse();
    }

    @Test
    void closeSession_success() {
        ArthasSession session = new ArthasSession();
        session.setId(1L);
        session.setServerId("server1");
        session.setArthasSessionId("arthas-session-1");
        session.setStatus("ACTIVE");

        ServerInfo serverInfo = createTestServerInfo();

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(serverService.findServerInfoById("server1")).thenReturn(Optional.of(serverInfo));
        when(arthasClient.executeSystemCommand(eq(serverInfo), eq("arthas-session-1"), eq("reset")))
                .thenReturn(null);
        when(arthasClient.closeSession(serverInfo, "arthas-session-1")).thenReturn(true);
        when(sessionRepository.save(any(ArthasSession.class))).thenReturn(session);

        boolean result = sessionService.closeSession(1L);

        assertThat(result).isTrue();
        verify(arthasClient).executeSystemCommand(eq(serverInfo), eq("arthas-session-1"), eq("reset"));
        verify(arthasClient).closeSession(serverInfo, "arthas-session-1");
        verify(sessionRepository).save(session);
        assertThat(session.getStatus()).isEqualTo("CLOSED");
        assertThat(session.getClosedAt()).isNotNull();
    }

    @Test
    void closeSession_sessionNotFound_returnsFalse() {
        when(sessionRepository.findById(99L)).thenReturn(Optional.empty());

        boolean result = sessionService.closeSession(99L);

        assertThat(result).isFalse();
    }

    @Test
    void cleanupOrphanSessions_success() {
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

        verify(sessionRepository).save(orphanSession);
        assertThat(orphanSession.getStatus()).isEqualTo("CLOSED");
    }

    @Test
    void cleanupStaleSessions_closesSessionsOlderThan10Minutes() {
        ArthasSession staleSession = new ArthasSession();
        staleSession.setId(1L);
        staleSession.setServerId("server1");
        staleSession.setArthasSessionId("arthas-session-1");
        staleSession.setStatus("ACTIVE");
        staleSession.setCreatedAt(LocalDateTime.now().minusMinutes(15));
        staleSession.setLastActiveAt(LocalDateTime.now().minusMinutes(5));

        when(sessionRepository.findByCreatedAtBeforeAndStatus(any(LocalDateTime.class), eq("ACTIVE")))
                .thenReturn(List.of(staleSession));
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(staleSession));
        ServerInfo serverInfo = createTestServerInfo();
        when(serverService.findServerInfoById("server1")).thenReturn(Optional.of(serverInfo));
        when(sessionRepository.save(any(ArthasSession.class))).thenReturn(staleSession);

        sessionService.cleanupStaleSessions();

        verify(sessionRepository).save(staleSession);
        assertThat(staleSession.getStatus()).isEqualTo("CLOSED");
    }

    @Test
    void cleanupStaleSessions_doesNotCloseRecentSessions() {
        ArthasSession recentSession = new ArthasSession();
        recentSession.setId(2L);
        recentSession.setServerId("server1");
        recentSession.setStatus("ACTIVE");
        recentSession.setCreatedAt(LocalDateTime.now().minusMinutes(5));

        when(sessionRepository.findByCreatedAtBeforeAndStatus(any(LocalDateTime.class), eq("ACTIVE")))
                .thenReturn(List.of());

        sessionService.cleanupStaleSessions();

        verify(sessionRepository, never()).save(any(ArthasSession.class));
    }

    @Test
    void cleanupStaleSessions_activeSessionNotClosed() {
        ArthasSession activeSession = new ArthasSession();
        activeSession.setId(3L);
        activeSession.setServerId("server1");
        activeSession.setArthasSessionId("arthas-session-3");
        activeSession.setStatus("ACTIVE");
        activeSession.setCreatedAt(LocalDateTime.now().minusMinutes(15));
        activeSession.setLastActiveAt(LocalDateTime.now().minusSeconds(30));

        when(sessionRepository.findByCreatedAtBeforeAndStatus(any(LocalDateTime.class), eq("ACTIVE")))
                .thenReturn(List.of(activeSession));
        when(sessionRepository.findById(3L)).thenReturn(Optional.of(activeSession));
        ServerInfo serverInfo = createTestServerInfo();
        when(serverService.findServerInfoById("server1")).thenReturn(Optional.of(serverInfo));
        when(sessionRepository.save(any(ArthasSession.class))).thenReturn(activeSession);

        sessionService.cleanupStaleSessions();

        verify(sessionRepository).save(activeSession);
        assertThat(activeSession.getStatus()).isEqualTo("CLOSED");
    }
}
