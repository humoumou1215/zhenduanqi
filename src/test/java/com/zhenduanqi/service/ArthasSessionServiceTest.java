package com.zhenduanqi.service;

import com.zhenduanqi.client.ArthasHttpClient;
import com.zhenduanqi.dto.ArthasSessionDTO;
import com.zhenduanqi.dto.CreateSessionRequest;
import com.zhenduanqi.entity.ArthasSession;
import com.zhenduanqi.model.ArthasApiResponse;
import com.zhenduanqi.model.SessionInfo;
import com.zhenduanqi.model.ServerInfo;
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
        sessionService = new ArthasSessionService(sessionRepository, serverService, arthasClient, commandGuardService);
    }

    private ServerInfo createTestServerInfo() {
        ServerInfo server = new ServerInfo();
        server.setId("server1");
        server.setName("Test Server");
        server.setHost("localhost");
        server.setHttpPort(8563);
        return server;
    }

    private SessionInfo createTestSessionInfo() {
        return new SessionInfo("test-session-id", "test-consumer-id");
    }

    private ArthasSession createTestSession() {
        ArthasSession session = new ArthasSession();
        session.setId(1L);
        session.setServerId("server1");
        session.setArthasSessionId("test-session-id");
        session.setArthasConsumerId("test-consumer-id");
        session.setStatus("ACTIVE");
        session.setUsername("test-user");
        session.setCommand("thread -n 5");
        session.setCreatedAt(LocalDateTime.now());
        session.setLastActiveAt(LocalDateTime.now());
        return session;
    }

    private CreateSessionRequest createTestRequest() {
        CreateSessionRequest request = new CreateSessionRequest();
        request.setServerId("server1");
        request.setCommand("thread -n 5");
        request.setSceneId(1L);
        request.setStepId(1L);
        request.setMaxExecTime(30000);
        return request;
    }

    @Test
    void createSession_serverNotFound_throwsException() {
        CreateSessionRequest request = createTestRequest();
        when(serverService.findServerInfoById("server1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.createSession(request, "test-user"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("未找到服务器");
    }

    @Test
    void createSession_dangerousCommand_isBlocked() {
        CreateSessionRequest request = createTestRequest();
        request.setCommand("ognl -x 1");
        
        when(serverService.findServerInfoById("server1")).thenReturn(Optional.of(createTestServerInfo()));
        when(commandGuardService.check("ognl -x 1")).thenReturn(
            new CommandGuardService.GuardResult(true, "高危命令已被拦截")
        );

        assertThatThrownBy(() -> sessionService.createSession(request, "test-user"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("命令被拦截");
    }

    @Test
    void createSession_initSessionFails_throwsException() {
        CreateSessionRequest request = createTestRequest();
        
        when(serverService.findServerInfoById("server1")).thenReturn(Optional.of(createTestServerInfo()));
        when(commandGuardService.check("thread -n 5")).thenReturn(new CommandGuardService.GuardResult(false, null));
        when(arthasClient.initSession(any())).thenReturn(null);

        assertThatThrownBy(() -> sessionService.createSession(request, "test-user"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("初始化 Arthas 会话失败");
    }

    @Test
    void createSession_success_createsSessionAndExecutesCommand() {
        CreateSessionRequest request = createTestRequest();
        ServerInfo serverInfo = createTestServerInfo();
        SessionInfo sessionInfo = createTestSessionInfo();
        ArthasSession savedSession = createTestSession();
        
        when(serverService.findServerInfoById("server1")).thenReturn(Optional.of(serverInfo));
        when(commandGuardService.check("thread -n 5")).thenReturn(new CommandGuardService.GuardResult(false, null));
        when(arthasClient.initSession(any())).thenReturn(sessionInfo);
        when(sessionRepository.save(any(ArthasSession.class))).thenReturn(savedSession);
        
        ArthasApiResponse apiResponse = new ArthasApiResponse();
        ArthasApiResponse.Body body = new ArthasApiResponse.Body();
        body.setJobId(123);
        apiResponse.setBody(body);
        when(arthasClient.asyncExecuteCommand(any(), eq("thread -n 5"), eq("test-session-id"))).thenReturn(apiResponse);

        ArthasSessionDTO result = sessionService.createSession(request, "test-user");

        assertThat(result).isNotNull();
        assertThat(result.getServerId()).isEqualTo("server1");
        assertThat(result.getUsername()).isEqualTo("test-user");
        verify(sessionRepository, times(2)).save(any(ArthasSession.class));
        verify(arthasClient).asyncExecuteCommand(any(), eq("thread -n 5"), eq("test-session-id"));
    }

    @Test
    void getActiveSessions_withServerId_returnsFilteredSessions() {
        ArthasSession session1 = createTestSession();
        ArthasSession session2 = createTestSession();
        session2.setId(2L);
        
        when(sessionRepository.findByServerIdAndStatusOrderByCreatedAtDesc("server1", "ACTIVE"))
            .thenReturn(List.of(session1, session2));

        List<ArthasSessionDTO> result = sessionService.getActiveSessions("server1");

        assertThat(result).hasSize(2);
        verify(sessionRepository).findByServerIdAndStatusOrderByCreatedAtDesc("server1", "ACTIVE");
    }

    @Test
    void getActiveSessions_withoutServerId_returnsAllActiveSessions() {
        ArthasSession session1 = createTestSession();
        ArthasSession session2 = createTestSession();
        session2.setId(2L);
        
        when(sessionRepository.findByStatusOrderByCreatedAtDesc("ACTIVE"))
            .thenReturn(List.of(session1, session2));

        List<ArthasSessionDTO> result = sessionService.getActiveSessions(null);

        assertThat(result).hasSize(2);
        verify(sessionRepository).findByStatusOrderByCreatedAtDesc("ACTIVE");
    }

    @Test
    void pullResults_sessionNotFound_returnsEmptyList() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.empty());

        List<?> result = sessionService.pullResults(1L);

        assertThat(result).isEmpty();
    }

    @Test
    void interruptJob_sessionNotFound_returnsFalse() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.empty());

        boolean result = sessionService.interruptJob(1L);

        assertThat(result).isFalse();
    }

    @Test
    void interruptJob_success_returnsTrue() {
        ArthasSession session = createTestSession();
        
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(serverService.findServerInfoById("server1")).thenReturn(Optional.of(createTestServerInfo()));
        when(arthasClient.interruptJob(any(), eq("test-session-id"))).thenReturn(true);

        boolean result = sessionService.interruptJob(1L);

        assertThat(result).isTrue();
        verify(arthasClient).interruptJob(any(), eq("test-session-id"));
    }

    @Test
    void closeSession_sessionNotFound_returnsFalse() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.empty());

        boolean result = sessionService.closeSession(1L);

        assertThat(result).isFalse();
    }

    @Test
    void closeSession_success_executesResetAndClosesSession() {
        ArthasSession session = createTestSession();
        
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(serverService.findServerInfoById("server1")).thenReturn(Optional.of(createTestServerInfo()));
        when(sessionRepository.save(any(ArthasSession.class))).thenReturn(session);

        boolean result = sessionService.closeSession(1L);

        assertThat(result).isTrue();
        verify(arthasClient).executeSystemCommand(any(), eq("test-session-id"), eq("reset"));
        verify(arthasClient).closeSession(any(), eq("test-session-id"));
        verify(sessionRepository).save(argThat(s -> s.getStatus().equals("CLOSED")));
    }
}
