package com.zhenduanqi.controller;

import com.zhenduanqi.dto.ArthasServerDTO;
import com.zhenduanqi.service.ArthasServerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<ArthasServerDTO> create(@RequestBody ArthasServerDTO dto) {
        ArthasServerDTO created = serverService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ArthasServerDTO> update(@PathVariable String id, @RequestBody ArthasServerDTO dto) {
        ArthasServerDTO updated = serverService.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        serverService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
