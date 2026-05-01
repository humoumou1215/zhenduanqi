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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ArthasSessionService {

    private static final Logger log = LoggerFactory.getLogger(ArthasSessionService.class);

    private final ArthasSessionRepository sessionRepository;
    private final ArthasServerService serverService;
    private final ArthasHttpClient arthasClient;
    private final CommandGuardService commandGuardService;

    public ArthasSessionService(ArthasSessionRepository sessionRepository,
                                ArthasServerService serverService,
                                ArthasHttpClient arthasClient,
                                CommandGuardService commandGuardService) {
        this.sessionRepository = sessionRepository;
        this.serverService = serverService;
        this.arthasClient = arthasClient;
        this.commandGuardService = commandGuardService;
    }

    public ArthasSessionDTO createSession(CreateSessionRequest request, String username) {
        Optional<ServerInfo> serverInfoOpt = serverService.findServerInfoById(request.getServerId());
        if (serverInfoOpt.isEmpty()) {
            throw new IllegalArgumentException("未找到服务器: " + request.getServerId());
        }

        CommandGuardService.GuardResult guardResult = commandGuardService.check(request.getCommand());
        if (guardResult.isBlocked()) {
            throw new IllegalArgumentException("命令被拦截: " + guardResult.getReason());
        }

        ServerInfo serverInfo = serverInfoOpt.get();
        SessionInfo sessionInfo = arthasClient.initSession(serverInfo);
        if (sessionInfo == null) {
            throw new RuntimeException("初始化 Arthas 会话失败");
        }

        ArthasSession session = new ArthasSession();
        session.setServerId(request.getServerId());
        session.setArthasSessionId(sessionInfo.getSessionId());
        session.setArthasConsumerId(sessionInfo.getConsumerId());
        session.setStatus("ACTIVE");
        session.setUsername(username);
        session.setSceneId(request.getSceneId());
        session.setStepId(request.getStepId());
        session.setCommand(request.getCommand());
        ArthasSession savedSession = sessionRepository.save(session);
        log.info("创建 Arthas 会话: sessionId={}, serverId={}, username={}", savedSession.getId(), request.getServerId(), username);

        ArthasApiResponse apiResponse = arthasClient.asyncExecuteCommand(serverInfo, request.getCommand(), sessionInfo.getSessionId());
        if (apiResponse != null && apiResponse.getBody() != null && apiResponse.getBody().getJobId() != null) {
            savedSession.setCurrentJobId(apiResponse.getBody().getJobId());
            savedSession = sessionRepository.save(savedSession);
        }

        return ArthasSessionDTO.fromEntity(savedSession);
    }

    public List<ArthasSessionDTO> getActiveSessions(String serverId) {
        List<ArthasSession> sessions;
        if (serverId != null && !serverId.isEmpty()) {
            sessions = sessionRepository.findByServerIdAndStatusOrderByCreatedAtDesc(serverId, "ACTIVE");
        } else {
            sessions = sessionRepository.findByStatusOrderByCreatedAtDesc("ACTIVE");
        }
        return sessions.stream().map(ArthasSessionDTO::fromEntity).collect(Collectors.toList());
    }

    public List<ArthasResult> pullResults(Long sessionId) {
        Optional<ArthasSession> sessionOpt = sessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            return List.of();
        }
        ArthasSession session = sessionOpt.get();
        Optional<ServerInfo> serverInfoOpt = serverService.findServerInfoById(session.getServerId());
        if (serverInfoOpt.isEmpty()) {
            return List.of();
        }
        return arthasClient.pullResults(serverInfoOpt.get(), session.getArthasSessionId(), session.getArthasConsumerId());
    }

    public boolean interruptJob(Long sessionId) {
        Optional<ArthasSession> sessionOpt = sessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            return false;
        }
        ArthasSession session = sessionOpt.get();
        Optional<ServerInfo> serverInfoOpt = serverService.findServerInfoById(session.getServerId());
        if (serverInfoOpt.isEmpty()) {
            return false;
        }
        boolean result = arthasClient.interruptJob(serverInfoOpt.get(), session.getArthasSessionId());
        log.info("中断任务: sessionId={}, result={}", sessionId, result);
        return result;
    }

    public boolean closeSession(Long sessionId) {
        Optional<ArthasSession> sessionOpt = sessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            return false;
        }
        ArthasSession session = sessionOpt.get();
        Optional<ServerInfo> serverInfoOpt = serverService.findServerInfoById(session.getServerId());
        if (serverInfoOpt.isPresent()) {
            try {
                arthasClient.executeSystemCommand(serverInfoOpt.get(), session.getArthasSessionId(), "reset");
            } catch (Exception e) {
                log.warn("执行 reset 命令失败", e);
            }
            arthasClient.closeSession(serverInfoOpt.get(), session.getArthasSessionId());
        }
        session.setStatus("CLOSED");
        session.setClosedAt(LocalDateTime.now());
        sessionRepository.save(session);
        log.info("关闭 Arthas 会话: sessionId={}", sessionId);
        return true;
    }

    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void cleanupOrphanSessions() {
        LocalDateTime tenMinutesAgo = LocalDateTime.now().minusMinutes(10);
        List<ArthasSession> orphanSessions = sessionRepository.findByLastActiveAtBeforeAndStatus(tenMinutesAgo, "ACTIVE");
        for (ArthasSession session : orphanSessions) {
            try {
                closeSession(session.getId());
            } catch (Exception e) {
                log.error("清理孤儿会话失败: sessionId={}", session.getId(), e);
            }
        }
        log.info("清理孤儿会话完成: count={}", orphanSessions.size());
    }
}
