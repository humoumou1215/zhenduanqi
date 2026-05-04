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
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ArthasSessionService {

    private static final Logger log = LoggerFactory.getLogger(ArthasSessionService.class);
    private static final int DEFAULT_MAX_EXEC_TIME = 30000;

    private final ArthasSessionRepository sessionRepository;
    private final ArthasServerService serverService;
    private final ArthasHttpClient arthasClient;
    private final CommandGuardService commandGuardService;
    private final ScheduledExecutorService timeoutScheduler = Executors.newScheduledThreadPool(1);
    private final Map<Long, ScheduledFuture<?>> timeoutTasks = new ConcurrentHashMap<>();

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

        int maxExecTime = request.getMaxExecTime() != null ? request.getMaxExecTime() : DEFAULT_MAX_EXEC_TIME;
        scheduleTimeoutTask(savedSession.getId(), maxExecTime);

        return ArthasSessionDTO.fromEntity(savedSession);
    }

    public List<ArthasSessionDTO> getActiveSessions(String serverId, String username) {
        List<ArthasSession> sessions;
        boolean hasServerId = serverId != null && !serverId.isEmpty();
        boolean hasUsername = username != null && !username.isEmpty();
        
        if (hasServerId && hasUsername) {
            sessions = sessionRepository.findByServerIdAndUsernameAndStatusOrderByCreatedAtDesc(serverId, username, "ACTIVE");
        } else if (hasServerId) {
            sessions = sessionRepository.findByServerIdAndStatusOrderByCreatedAtDesc(serverId, "ACTIVE");
        } else if (hasUsername) {
            sessions = sessionRepository.findByUsernameAndStatusOrderByCreatedAtDesc(username, "ACTIVE");
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
        
        session.setLastActiveAt(LocalDateTime.now());
        sessionRepository.save(session);
        
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
        cancelTimeoutTask(sessionId);
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
        cancelTimeoutTask(sessionId);
        return true;
    }

    private void scheduleTimeoutTask(Long sessionId, int maxExecTimeMs) {
        ScheduledFuture<?> future = timeoutScheduler.schedule(() -> {
            log.info("命令执行超时，开始中断: sessionId={}", sessionId);
            try {
                interruptJob(sessionId);
                closeSession(sessionId);
            } catch (Exception e) {
                log.error("超时处理失败: sessionId={}", sessionId, e);
            }
        }, maxExecTimeMs, TimeUnit.MILLISECONDS);
        timeoutTasks.put(sessionId, future);
    }

    private void cancelTimeoutTask(Long sessionId) {
        ScheduledFuture<?> future = timeoutTasks.remove(sessionId);
        if (future != null && !future.isDone()) {
            future.cancel(false);
            log.debug("取消超时任务: sessionId={}", sessionId);
        }
    }

    public ArthasSessionDTO reconnectSession(Long sessionId) {
        Optional<ArthasSession> sessionOpt = sessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            throw new IllegalArgumentException("会话不存在");
        }

        ArthasSession session = sessionOpt.get();
        Optional<ServerInfo> serverInfoOpt = serverService.findServerInfoById(session.getServerId());
        if (serverInfoOpt.isEmpty()) {
            throw new IllegalArgumentException("服务器不存在");
        }

        ServerInfo serverInfo = serverInfoOpt.get();
        String newConsumerId = arthasClient.joinSession(serverInfo, session.getArthasSessionId());
        if (newConsumerId == null) {
            throw new RuntimeException("重连会话失败");
        }

        session.setArthasConsumerId(newConsumerId);
        session.setLastActiveAt(LocalDateTime.now());
        ArthasSession updatedSession = sessionRepository.save(session);
        log.info("重连会话成功: sessionId={}, newConsumerId={}", sessionId, newConsumerId);

        return ArthasSessionDTO.fromEntity(updatedSession);
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

    @Scheduled(fixedRate = 1 * 60 * 1000)
    public void cleanupStaleSessions() {
        LocalDateTime tenMinutesAgo = LocalDateTime.now().minusMinutes(10);
        List<ArthasSession> staleSessions = sessionRepository.findByCreatedAtBeforeAndStatus(tenMinutesAgo, "ACTIVE");
        for (ArthasSession session : staleSessions) {
            try {
                log.warn("会话级超时，开始清理: sessionId={}, createdAt={}", session.getId(), session.getCreatedAt());
                interruptJob(session.getId());
                closeSession(session.getId());
                log.info("会话级超时清理完成: sessionId={}", session.getId());
            } catch (Exception e) {
                log.error("会话级超时清理失败: sessionId={}", session.getId(), e);
            }
        }
        if (!staleSessions.isEmpty()) {
            log.info("会话级超时清理完成: count={}", staleSessions.size());
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void cleanupOnStartup() {
        log.info("服务启动，开始清理所有遗留的活跃会话...");
        try {
            cleanupOrphanSessions();
            cleanupStaleSessions();
            log.info("启动清理完成");
        } catch (Exception e) {
            log.error("启动清理失败", e);
        }
    }
}
