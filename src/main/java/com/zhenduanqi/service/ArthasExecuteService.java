package com.zhenduanqi.service;

import com.zhenduanqi.client.ArthasHttpClient;
import com.zhenduanqi.dto.ExecuteResponse;
import com.zhenduanqi.entity.ArthasServerEntity;
import com.zhenduanqi.model.ServerInfo;
import com.zhenduanqi.repository.ArthasServerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class ArthasExecuteService {

    private static final Logger log = LoggerFactory.getLogger(ArthasExecuteService.class);

    private final ArthasServerRepository serverRepository;
    private final ArthasServerService serverService;
    private final ArthasHttpClient arthasClient;
    private final CommandGuardService commandGuardService;

    public ArthasExecuteService(ArthasServerRepository serverRepository,
                                ArthasServerService serverService,
                                ArthasHttpClient arthasClient,
                                CommandGuardService commandGuardService) {
        this.serverRepository = serverRepository;
        this.serverService = serverService;
        this.arthasClient = arthasClient;
        this.commandGuardService = commandGuardService;
    }

    private ExecuteResponse doExecute(String serverId, String command) {
        Optional<ArthasServerEntity> entityOpt = serverRepository.findById(serverId);
        if (entityOpt.isEmpty()) {
            ExecuteResponse resp = new ExecuteResponse();
            resp.setState("failed");
            resp.setError("未找到服务器: " + serverId);
            return resp;
        }

        ArthasServerEntity entity = entityOpt.get();
        Optional<String> decryptedTokenOpt = serverService.findDecryptedTokenById(serverId);
        if (decryptedTokenOpt.isEmpty()) {
            ExecuteResponse resp = new ExecuteResponse();
            resp.setState("failed");
            resp.setError("无法获取服务器 token: " + serverId);
            return resp;
        }

        ServerInfo serverInfo = new ServerInfo();
        serverInfo.setId(entity.getId());
        serverInfo.setName(entity.getName());
        serverInfo.setHost(entity.getHost());
        serverInfo.setHttpPort(entity.getHttpPort());
        serverInfo.setToken(decryptedTokenOpt.get());

        var arthasResp = arthasClient.executeCommand(serverInfo, command);
        return ExecuteResponse.fromArthasResponse(arthasResp);
    }

    public ExecuteResponse execute(String serverId, String command) {
        log.info("命令执行开始: serverId={}, command={}", serverId, command.length() > 50 ? command.substring(0, 50) + "..." : command);
        long start = System.currentTimeMillis();

        CommandGuardService.GuardResult guardResult = commandGuardService.check(command);
        if (guardResult.isBlocked()) {
            ExecuteResponse resp = new ExecuteResponse();
            resp.setState("blocked");
            resp.setError(guardResult.getReason());
            return resp;
        }
        ExecuteResponse result = doExecute(serverId, command);
        long duration = System.currentTimeMillis() - start;
        log.info("命令执行完成: serverId={}, state={}, duration={}ms", serverId, result.getState(), duration);
        return result;
    }

    public ExecuteResponse executeSystemCommand(String serverId, String command) {
        return doExecute(serverId, command);
    }

    public Map<String, Object> getStatus(String serverId) {
        return serverRepository.findById(serverId)
                .map(entity -> {
                    Optional<String> decryptedTokenOpt = serverService.findDecryptedTokenById(serverId);
                    if (decryptedTokenOpt.isEmpty()) {
                        Map<String, Object> m = new HashMap<>();
                        m.put("exists", true);
                        m.put("id", entity.getId());
                        m.put("name", entity.getName());
                        m.put("host", entity.getHost());
                        m.put("port", entity.getHttpPort());
                        m.put("connected", false);
                        return m;
                    }

                    ServerInfo serverInfo = new ServerInfo();
                    serverInfo.setId(entity.getId());
                    serverInfo.setName(entity.getName());
                    serverInfo.setHost(entity.getHost());
                    serverInfo.setHttpPort(entity.getHttpPort());
                    serverInfo.setToken(decryptedTokenOpt.get());
                    boolean connected = arthasClient.checkConnection(serverInfo);

                    Map<String, Object> m = new HashMap<>();
                    m.put("exists", true);
                    m.put("id", entity.getId());
                    m.put("name", entity.getName());
                    m.put("host", entity.getHost());
                    m.put("port", entity.getHttpPort());
                    m.put("connected", connected);
                    return m;
                })
                .orElseGet(() -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("exists", false);
                    return m;
                });
    }
}
