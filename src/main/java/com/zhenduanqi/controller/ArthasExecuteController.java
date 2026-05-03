package com.zhenduanqi.controller;

import com.zhenduanqi.annotation.AuditLog;
import com.zhenduanqi.annotation.RequireRole;
import com.zhenduanqi.dto.ExecuteRequest;
import com.zhenduanqi.dto.ExecuteResponse;
import com.zhenduanqi.service.ArthasExecuteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ArthasExecuteController {

    private final ArthasExecuteService executeService;

    public ArthasExecuteController(ArthasExecuteService executeService) {
        this.executeService = executeService;
    }

    @PostMapping("/execute")
    @RequireRole({"OPERATOR", "ADMIN"})
    @AuditLog(action = "执行诊断命令")
    public ResponseEntity<ExecuteResponse> execute(@RequestBody ExecuteRequest request) {
        ExecuteResponse response = executeService.execute(request.getServerId(), request.getCommand());
        return ResponseEntity.ok(response);
    }
}
