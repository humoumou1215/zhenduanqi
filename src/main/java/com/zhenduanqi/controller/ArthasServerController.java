package com.zhenduanqi.controller;

import com.zhenduanqi.annotation.AuditLog;
import com.zhenduanqi.annotation.RequireRole;
import com.zhenduanqi.dto.ArthasServerDTO;
import com.zhenduanqi.dto.ServerStatusDTO;
import com.zhenduanqi.service.ArthasServerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/servers")
public class ArthasServerController {

    private final ArthasServerService serverService;

    public ArthasServerController(ArthasServerService serverService) {
        this.serverService = serverService;
    }

    @GetMapping
    public List<ArthasServerDTO> list() {
        return serverService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArthasServerDTO> getById(@PathVariable String id) {
        return serverService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @RequireRole("ADMIN")
    @AuditLog(action = "创建服务器")
    public ResponseEntity<?> create(@RequestBody ArthasServerDTO dto) {
        try {
            ArthasServerDTO created = serverService.create(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @RequireRole("ADMIN")
    @AuditLog(action = "更新服务器")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody ArthasServerDTO dto) {
        try {
            ArthasServerDTO updated = serverService.update(id, dto);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("不存在")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
            }
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @RequireRole("ADMIN")
    @AuditLog(action = "删除服务器")
    public ResponseEntity<?> delete(@PathVariable String id) {
        try {
            serverService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            if (e.getMessage().contains("不存在")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
            }
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<ServerStatusDTO> checkStatus(@PathVariable String id) {
        ServerStatusDTO status = serverService.checkConnection(id);
        return ResponseEntity.ok(status);
    }
}
