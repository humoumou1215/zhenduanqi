package com.zhenduanqi.service;

import com.zhenduanqi.client.ArthasHttpClient;
import com.zhenduanqi.model.ArthasResponse;
import com.zhenduanqi.entity.ArthasServerEntity;
import com.zhenduanqi.repository.ArthasServerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArthasExecuteServiceTest {

    @Mock
    private ArthasServerRepository serverRepository;

    @Mock
    private ArthasServerService serverService;

    @Mock
    private ArthasHttpClient arthasClient;

    @Mock
    private CommandGuardService commandGuardService;

    private ArthasExecuteService executeService;

    @BeforeEach
    void setUp() {
        executeService = new ArthasExecuteService(serverRepository, serverService, arthasClient, commandGuardService);
    }

    private ArthasServerEntity createTestServer() {
        ArthasServerEntity server = new ArthasServerEntity();
        server.setId("server1");
        server.setName("Test Server");
        server.setHost("localhost");
        server.setHttpPort(8563);
        server.setToken("test-token");
        return server;
    }

    @Test
    void execute_dangerousCommand_isBlocked() {
        when(commandGuardService.check("ognl -x 1")).thenReturn(
            new CommandGuardService.GuardResult(true, "高危命令已被拦截: ognl")
        );

        var response = executeService.execute("server1", "ognl -x 1");

        assertThat(response.getState()).isEqualTo("blocked");
        assertThat(response.getError()).contains("高危命令已被拦截");
        verify(arthasClient, never()).executeCommand(any(), any());
    }

    @Test
    void execute_safeCommand_executesSuccessfully() {
        ArthasServerEntity server = createTestServer();

        when(serverRepository.findById("server1")).thenReturn(Optional.of(server));
        when(serverService.findDecryptedTokenById("server1")).thenReturn(Optional.of("test-token"));
        when(commandGuardService.check("thread -n 5")).thenReturn(
            new CommandGuardService.GuardResult(false, null)
        );

        ArthasResponse arthasResponse = new ArthasResponse();
        arthasResponse.setState("success");
        when(arthasClient.executeCommand(any(), eq("thread -n 5"))).thenReturn(arthasResponse);

        var response = executeService.execute("server1", "thread -n 5");

        assertThat(response.getState()).isEqualTo("success");
        verify(arthasClient).executeCommand(any(), eq("thread -n 5"));
    }

    @Test
    void execute_whitelistedCommand_executesDespiteBlacklist() {
        ArthasServerEntity server = createTestServer();

        when(serverRepository.findById("server1")).thenReturn(Optional.of(server));
        when(serverService.findDecryptedTokenById("server1")).thenReturn(Optional.of("test-token"));
        when(commandGuardService.check("jad --source-only com.example.Class")).thenReturn(
            new CommandGuardService.GuardResult(false, null)
        );

        ArthasResponse arthasResponse = new ArthasResponse();
        arthasResponse.setState("success");
        when(arthasClient.executeCommand(any(), eq("jad --source-only com.example.Class"))).thenReturn(arthasResponse);

        var response = executeService.execute("server1", "jad --source-only com.example.Class");

        assertThat(response.getState()).isEqualTo("success");
        verify(arthasClient).executeCommand(any(), eq("jad --source-only com.example.Class"));
    }

    @Test
    void executeSystemCommand_bypassesGuardAndExecutes() {
        ArthasServerEntity server = createTestServer();

        when(serverRepository.findById("server1")).thenReturn(Optional.of(server));
        when(serverService.findDecryptedTokenById("server1")).thenReturn(Optional.of("test-token"));

        ArthasResponse arthasResponse = new ArthasResponse();
        arthasResponse.setState("succeeded");
        when(arthasClient.executeCommand(any(), eq("reset"))).thenReturn(arthasResponse);

        var response = executeService.executeSystemCommand("server1", "reset");

        assertThat(response.getState()).isEqualTo("succeeded");
        verify(commandGuardService, never()).check(any());
        verify(arthasClient).executeCommand(any(), eq("reset"));
    }

    @Test
    void executeSystemCommand_dangerousCommand_bypassesGuard() {
        ArthasServerEntity server = createTestServer();

        when(serverRepository.findById("server1")).thenReturn(Optional.of(server));
        when(serverService.findDecryptedTokenById("server1")).thenReturn(Optional.of("test-token"));

        ArthasResponse arthasResponse = new ArthasResponse();
        arthasResponse.setState("succeeded");
        when(arthasClient.executeCommand(any(), eq("ognl -x 1"))).thenReturn(arthasResponse);

        var response = executeService.executeSystemCommand("server1", "ognl -x 1");

        assertThat(response.getState()).isEqualTo("succeeded");
        verify(commandGuardService, never()).check(any());
        verify(arthasClient).executeCommand(any(), eq("ognl -x 1"));
    }

    @Test
    void executeSystemCommand_serverNotFound_returnsFailed() {
        when(serverRepository.findById("nonexistent")).thenReturn(Optional.empty());

        var response = executeService.executeSystemCommand("nonexistent", "version");

        assertThat(response.getState()).isEqualTo("failed");
        assertThat(response.getError()).contains("未找到服务器");
        verify(arthasClient, never()).executeCommand(any(), any());
    }
}
