package com.zhenduanqi.controller;

import com.zhenduanqi.dto.ExecuteRequest;
import com.zhenduanqi.dto.ExecuteResponse;
import com.zhenduanqi.service.ArthasExecuteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ArthasExecuteController {

    private final ArthasExecuteService executeService;

    public ArthasExecuteController(ArthasExecuteService executeService) {
        this.executeService = executeService;
    }

    @PostMapping("/execute")
    public ResponseEntity<ExecuteResponse> execute(@RequestBody ExecuteRequest request) {
        ExecuteResponse response = executeService.execute(request.getServerId(), request.getCommand());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/servers/{id}/status")
    public ResponseEntity<Map<String, Object>> status(@PathVariable String id) {
        Map<String, Object> status = executeService.getStatus(id);
        return ResponseEntity.ok(status);
    }
}
