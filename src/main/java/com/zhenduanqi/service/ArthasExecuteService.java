package com.zhenduanqi.service;

import com.zhenduanqi.client.ArthasHttpClient;
import com.zhenduanqi.dto.ExecuteResponse;
import com.zhenduanqi.entity.ArthasServerEntity;
import com.zhenduanqi.model.ServerInfo;
import com.zhenduanqi.repository.ArthasServerRepository;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class ArthasExecuteService {

    private final ArthasServerRepository serverRepository;
    private final ArthasHttpClient arthasClient;

    public ArthasExecuteService(ArthasServerRepository serverRepository, ArthasHttpClient arthasClient) {
        this.serverRepository = serverRepository;
        this.arthasClient = arthasClient;
    }

    public ExecuteResponse execute(String serverId, String command) {
        Optional<ArthasServerEntity> entityOpt = serverRepository.findById(serverId);
        if (entityOpt.isEmpty()) {
            ExecuteResponse resp = new ExecuteResponse();
            resp.setState("failed");
            resp.setError("未找到服务器: " + serverId);
            return resp;
        }

        ArthasServerEntity entity = entityOpt.get();
        ServerInfo serverInfo = new ServerInfo();
        serverInfo.setId(entity.getId());
        serverInfo.setName(entity.getName());
        serverInfo.setHost(entity.getHost());
        serverInfo.setHttpPort(entity.getHttpPort());
        serverInfo.setToken(entity.getToken());

        var arthasResp = arthasClient.executeCommand(serverInfo, command);
        return ExecuteResponse.fromArthasResponse(arthasResp);
    }

    public Map<String, Object> getStatus(String serverId) {
        return serverRepository.findById(serverId)
                .map(entity -> {
                    ServerInfo serverInfo = new ServerInfo();
                    serverInfo.setId(entity.getId());
                    serverInfo.setName(entity.getName());
                    serverInfo.setHost(entity.getHost());
                    serverInfo.setHttpPort(entity.getHttpPort());
                    serverInfo.setToken(entity.getToken());
                    boolean connected = arthasClient.checkConnection(serverInfo);
                    return Map.of(
                            "exists", true,
                            "id", entity.getId(),
                            "name", entity.getName(),
                            "host", entity.getHost(),
                            "port", entity.getHttpPort(),
                            "connected", connected
                    );
                })
                .orElseGet(() -> Map.of("exists", false));
    }
}
