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
        Optional<ServerInfo> serverInfoOpt = serverService.findServerInfoById(serverId);
        if (serverInfoOpt.isEmpty()) {
            ExecuteResponse resp = new ExecuteResponse();
            resp.setState("failed");
            resp.setError("未找到服务器: " + serverId);
            return resp;
        }

        var arthasResp = arthasClient.executeCommand(serverInfoOpt.get(), command);
        return ExecuteResponse.fromArthasResponse(arthasResp);
    }

    public ExecuteResponse execute(String serverId, String command) {
        String commandSummary = summarizeCommand(command);
        log.info("CommandChain: 接收命令执行请求, serverId={}, command={}", serverId, commandSummary);
        long start = System.currentTimeMillis();

        CommandGuardService.GuardResult guardResult = commandGuardService.check(command);
        if (guardResult.isBlocked()) {
            log.warn("CommandChain: 命令被拦截, serverId={}, command={}, reason={}", 
                    serverId, commandSummary, guardResult.getReason());
            ExecuteResponse resp = new ExecuteResponse();
            resp.setState("blocked");
            resp.setError(guardResult.getReason());
            return resp;
        }
        
        log.info("CommandChain: 校验通过，发送到 Arthas, serverId={}, command={}", serverId, commandSummary);
        ExecuteResponse result = doExecute(serverId, command);
        long duration = System.currentTimeMillis() - start;
        
        log.info("CommandChain: 收到响应, serverId={}, state={}, duration={}ms, command={}", 
                serverId, result.getState(), duration, commandSummary);
        return result;
    }
    
    private String summarizeCommand(String command) {
        if (command == null) return "";
        if (command.length() <= 50) return command;
        String firstWord = command.split("\\s+")[0];
        return firstWord + "...";
    }

    public ExecuteResponse executeSystemCommand(String serverId, String command) {
        return doExecute(serverId, command);
    }

    public Map<String, Object> getStatus(String serverId) {
        return serverRepository.findById(serverId)
                .map(entity -> {
                    Optional<ServerInfo> serverInfoOpt = serverService.findServerInfoById(serverId);
                    boolean connected = false;
                    if (serverInfoOpt.isPresent()) {
                        connected = arthasClient.checkConnection(serverInfoOpt.get());
                    }

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
