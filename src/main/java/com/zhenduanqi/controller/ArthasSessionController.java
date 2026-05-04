package com.zhenduanqi.controller;

import com.zhenduanqi.annotation.AuditLog;
import com.zhenduanqi.annotation.RequireRole;
import com.zhenduanqi.dto.ArthasSessionDTO;
import com.zhenduanqi.dto.CreateSessionRequest;
import com.zhenduanqi.model.ArthasResult;
import com.zhenduanqi.service.ArthasSessionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/arthas-sessions")
public class ArthasSessionController {

    private final ArthasSessionService sessionService;

    public ArthasSessionController(ArthasSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    @RequireRole({"OPERATOR", "ADMIN"})
    @AuditLog(action = "创建 Arthas 会话")
    public ResponseEntity<ArthasSessionDTO> createSession(@RequestBody CreateSessionRequest request,
                                                          HttpServletRequest httpRequest) {
        String username = (String) httpRequest.getAttribute("username");
        ArthasSessionDTO session = sessionService.createSession(request, username);
        return ResponseEntity.ok(session);
    }

    @GetMapping
    @RequireRole("ADMIN")
    public ResponseEntity<List<ArthasSessionDTO>> getActiveSessions(@RequestParam(required = false) String serverId,
                                                                 @RequestParam(required = false) String username) {
        List<ArthasSessionDTO> sessions = sessionService.getActiveSessions(serverId, username);
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/{id}/results")
    @RequireRole({"OPERATOR", "ADMIN"})
    public ResponseEntity<List<ArthasResult>> pullResults(@PathVariable Long id) {
        List<ArthasResult> results = sessionService.pullResults(id);
        return ResponseEntity.ok(results);
    }

    @PostMapping("/{id}/interrupt")
    @RequireRole({"OPERATOR", "ADMIN"})
    @AuditLog(action = "中断 Arthas 任务")
    public ResponseEntity<Map<String, Boolean>> interruptJob(@PathVariable Long id) {
        boolean result = sessionService.interruptJob(id);
        return ResponseEntity.ok(Map.of("success", result));
    }

    @PostMapping("/{id}/close")
    @RequireRole({"OPERATOR", "ADMIN"})
    @AuditLog(action = "关闭 Arthas 会话")
    public ResponseEntity<Map<String, Boolean>> closeSession(@PathVariable Long id) {
        boolean result = sessionService.closeSession(id);
        return ResponseEntity.ok(Map.of("success", result));
    }

    @PostMapping("/{id}/reconnect")
    @RequireRole({"OPERATOR", "ADMIN"})
    @AuditLog(action = "重连 Arthas 会话")
    public ResponseEntity<ArthasSessionDTO> reconnectSession(@PathVariable Long id) {
        ArthasSessionDTO session = sessionService.reconnectSession(id);
        return ResponseEntity.ok(session);
    }
}
